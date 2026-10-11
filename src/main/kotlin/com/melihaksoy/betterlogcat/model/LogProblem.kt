package com.melihaksoy.betterlogcat.model

/** What makes a log line a problem. Order is severity, worst first, and the Problems panel order. */
enum class ProblemKind {
    /** A Java or Kotlin crash (FATAL EXCEPTION), an assert, or a native crash (Fatal signal). */
    CRASH,

    /** "Application Not Responding", from ActivityManager. */
    ANR,
    EXCEPTION,

    /** A StrictMode policy violation, like disk or network access on the main thread. */
    STRICT_MODE,
    WARNING,
}

/** Finds crashes, ANRs, exceptions, StrictMode violations and warnings in log lines. */
object LogProblems {

    /** The worst kind [entry] is. Null for lines that are none. */
    fun kindOf(entry: LogEntry): ProblemKind? = when {
        entry.isCrash -> ProblemKind.CRASH
        isAnr(tag = entry.tag, message = entry.message) -> ProblemKind.ANR
        isStrictMode(tag = entry.tag, message = entry.message) -> ProblemKind.STRICT_MODE
        entry.exceptionName != null -> ProblemKind.EXCEPTION
        entry.level == LogLevel.WARN -> ProblemKind.WARNING
        else -> null
    }

    /** A native crash: libc's "Fatal signal 11 (SIGSEGV), code 1 …" line. */
    fun isNativeCrash(
        tag: String,
        message: String,
    ): Boolean = tag == "libc" && message.startsWith(prefix = "Fatal signal")

    /** The signal name of a native crash, like "SIGSEGV". */
    fun signalName(message: String): String? = SIGNAL.find(input = message)?.groupValues?.get(1)

    fun isAnr(
        tag: String,
        message: String,
    ): Boolean = tag == "ActivityManager" && message.startsWith(prefix = "ANR in ")

    fun isStrictMode(
        tag: String,
        message: String,
    ): Boolean = tag == "StrictMode" && message.contains(other = "policy violation")

    /** The StrictMode violation class, like "DiskReadViolation". */
    fun strictModeViolation(message: String): String? = VIOLATION.find(input = message)?.groupValues?.get(1)

    /**
     * The first throwable class [message] names, like `java.lang.IllegalStateException` in
     * "java.lang.IllegalStateException: Unable to create a fetcher". The name must be qualified and
     * followed by a colon or the line end, so "ErrorHandler" or "no Error here" do not count.
     */
    fun exceptionName(message: String): String? {
        val head = if (message.length > SCAN_CHARS) message.substring(0, SCAN_CHARS) else message
        if (HINTS.none { head.contains(other = it) }) return null
        return THROWABLE.find(input = head)?.groupValues?.get(1)
    }

    /** The text after the throwable name on its line, like "Unable to create a fetcher". */
    fun exceptionDetail(
        message: String,
        name: String,
    ): String {
        val start = message.indexOf(string = name)
        if (start < 0) return ""
        val line = message.substring(startIndex = start + name.length).lineSequence().first()
        return line.removePrefix(prefix = ":").trim()
    }

    // Exception headers are near the start; a huge JSON body needs no full scan.
    private const val SCAN_CHARS = 4_000
    private val HINTS = listOf("Exception", "Error", "Throwable")
    private val THROWABLE = Regex(pattern = """(?<![\w$.])((?:[a-z_][\w$]*\.)+(?=[A-Z])[\w$]*(?:Exception|Error|Throwable))(?=:|\s*$)""", option = RegexOption.MULTILINE)
    private val SIGNAL = Regex(pattern = """\((SIG[A-Z]+)\)""")
    private val VIOLATION = Regex(pattern = """android\.os\.strictmode\.(\w+)""")
}
