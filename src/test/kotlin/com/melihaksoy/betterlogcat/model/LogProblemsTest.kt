package com.melihaksoy.betterlogcat.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogProblemsTest {

    private fun entry(
        id: Long,
        level: LogLevel,
        tag: String,
        message: String,
    ) = LogEntry(
        id = id,
        epochSeconds = 1_791_540_000L + id,
        nanos = 0,
        pid = 1,
        tid = 1,
        level = level,
        tag = tag,
        message = message,
    )

    @Test
    fun `finds qualified throwable names before a colon or the line end`() {
        assertEquals(
            "java.lang.IllegalStateException",
            LogProblems.exceptionName(message = "java.lang.IllegalStateException: Unable to create a fetcher\n\tat coil3.Foo.bar(Foo.kt:1)"),
        )
        assertEquals("java.lang.Error", LogProblems.exceptionName(message = "Caused by: java.lang.Error"))
        assertEquals("java.net.UnknownHostException", LogProblems.exceptionName(message = "<-- HTTP FAILED: java.net.UnknownHostException: Unable to resolve host"))
        assertEquals("a.B\$InnerException", LogProblems.exceptionName(message = "a.B\$InnerException: x"))
    }

    @Test
    fun `plain words and frames are not throwables`() {
        assertNull(LogProblems.exceptionName(message = "Error loading image"))
        assertNull(LogProblems.exceptionName(message = "com.app.ErrorHandler: ready"))
        assertNull(LogProblems.exceptionName(message = "IllegalStateException: no package"))
        assertNull(LogProblems.exceptionName(message = "at com.app.ExceptionMapper.map(ExceptionMapper.kt:12)"))
    }

    @Test
    fun `kind is crash, then exception, then warning`() {
        val crash = entry(id = 1, level = LogLevel.ERROR, tag = "AndroidRuntime", message = "FATAL EXCEPTION: main\njava.lang.NullPointerException: x")
        val exception = entry(id = 2, level = LogLevel.DEBUG, tag = "Loader", message = "Failed - java.io.IOException: closed")
        val warning = entry(id = 3, level = LogLevel.WARN, tag = "Choreographer", message = "Skipped 42 frames!")
        val error = entry(id = 4, level = LogLevel.ERROR, tag = "Loader", message = "Failed -")

        assertEquals(ProblemKind.CRASH, crash.problem)
        assertEquals(ProblemKind.EXCEPTION, exception.problem)
        assertEquals(ProblemKind.WARNING, warning.problem)
        assertNull(error.problem)
        assertEquals("closed", LogProblems.exceptionDetail(message = exception.message, name = "java.io.IOException"))
    }

    @Test
    fun `anr, strict mode and native crashes have their own kinds and names`() {
        val anr = entry(id = 1, level = LogLevel.ERROR, tag = "ActivityManager", message = "ANR in com.app (com.app/.MainActivity)\nPID: 123\nReason: Input dispatching timed out")
        val strict = entry(id = 2, level = LogLevel.DEBUG, tag = "StrictMode", message = "StrictMode policy violation; ~duration=48 ms: android.os.strictmode.DiskReadViolation\n\tat android.os.StrictMode.onReadFromDisk(StrictMode.java:1)")
        val native = entry(id = 3, level = LogLevel.ASSERT, tag = "libc", message = "Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0 in tid 123 (main)")

        assertEquals(ProblemKind.ANR, anr.problem)
        assertEquals("Input dispatching timed out", ProblemDescriptions.describe(entry = anr, kind = ProblemKind.ANR).detail)
        assertEquals(ProblemKind.STRICT_MODE, strict.problem)
        assertEquals("DiskReadViolation", ProblemDescriptions.describe(entry = strict, kind = ProblemKind.STRICT_MODE).title)
        assertEquals(ProblemKind.CRASH, native.problem)
        assertEquals("SIGSEGV", ProblemDescriptions.describe(entry = native, kind = ProblemKind.CRASH).title)
    }

    @Test
    fun `the recorder groups the same problem, worst kind first, newest first`() {
        val recorder = ProblemRecorder()
        listOf(
            entry(id = 1, level = LogLevel.WARN, tag = "Choreographer", message = "Skipped 42 frames!"),
            entry(id = 2, level = LogLevel.ERROR, tag = "Loader", message = "java.io.IOException: closed"),
            entry(id = 3, level = LogLevel.WARN, tag = "Choreographer", message = "Skipped 7 frames!"),
            entry(id = 4, level = LogLevel.WARN, tag = "Other", message = "Low memory"),
            entry(id = 5, level = LogLevel.ERROR, tag = "Loader", message = "java.io.IOException: reset"),
        ).forEach { recorder.record(entry = it, processName = "com.app", ignoredKeys = emptySet()) }

        val problems = recorder.snapshot()

        assertEquals(listOf("IOException", "Low memory", "Skipped 7 frames!"), problems.map { it.title })
        assertEquals(2, problems[0].count)
        assertEquals("reset", problems[0].detail)
        assertEquals(listOf(2L, 5L), recorder.liveIds(key = problems[0].key))
        assertEquals(listOf(42, 7), recorder.frameSkips().map { it.frames })
    }

    @Test
    fun `the recorder skips ignored problems`() {
        val recorder = ProblemRecorder()
        val warning = entry(id = 1, level = LogLevel.WARN, tag = "Other", message = "Low memory")
        val key = ProblemDescriptions.describe(entry = warning, kind = ProblemKind.WARNING).key

        assertNull(recorder.record(entry = warning, processName = "", ignoredKeys = setOf(key)))
        assertEquals(0, recorder.size)
    }
}
