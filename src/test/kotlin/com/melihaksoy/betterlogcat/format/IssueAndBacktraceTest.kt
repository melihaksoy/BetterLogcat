package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.Device
import com.melihaksoy.betterlogcat.model.DeviceState
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueAndBacktraceTest {

    @Test
    fun `issue markdown has the crash, app version, device and trace`() {
        val crash = LogEntry(
            id = 1, epochSeconds = 1_791_540_000L, nanos = 0, pid = 1, tid = 1, level = LogLevel.ERROR, tag = "AndroidRuntime",
            message = "FATAL EXCEPTION: main\njava.lang.IllegalStateException: Listing id must not be blank\n\tat a.B.c(B.kt:1)",
        )

        val markdown = IssueReport.markdown(
            entry = crash,
            packageName = "com.app",
            version = IssueReport.parseVersion(dumpsys = "    versionCode=45 minSdk=26 targetSdk=35\n    versionName=1.2.3\n"),
            device = Device(serial = "x", state = DeviceState.ONLINE, name = "Pixel 9", androidVersion = "15", apiLevel = "35"),
        )

        assertTrue(markdown.startsWith("### Crash: IllegalStateException\n\nListing id must not be blank\n"))
        assertTrue("- **App:** `com.app` 1.2.3 (45)" in markdown)
        assertTrue("- **Device:** Pixel 9, Android 15 (API 35)" in markdown)
        assertTrue("```\nFATAL EXCEPTION: main\n" in markdown)
    }

    @Test
    fun `backtrace frames and abi are read from crash_dump lines`() {
        val lines = listOf(
            "ABI: 'arm64'",
            "      #00 pc 000000000004f3c8  /data/app/~~x/com.app-y/lib/arm64/libgame.so (update+24) (BuildId: abc)",
            "      #01 pc 00000000000d2a10  /apex/com.android.runtime/lib64/bionic/libc.so (__start_thread+64)",
        )

        val frames = NativeBacktrace.frames(lines = lines)

        assertEquals(listOf("libgame.so", "libc.so"), frames.map { it.libraryName })
        assertEquals("000000000004f3c8", frames[0].pc)
        assertEquals("update+24", frames[0].symbol)
        assertEquals("arm64-v8a", NativeBacktrace.abiFolder(lines = lines))
    }
}
