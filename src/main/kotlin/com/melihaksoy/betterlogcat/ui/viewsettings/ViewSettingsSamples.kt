package com.melihaksoy.betterlogcat.ui.viewsettings

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel

/** Fixed rows for the live preview in the view settings window. */
internal object ViewSettingsSamples {

    private const val APP_PID = 21_430
    private const val SYSTEM_PID = 1_822

    val processNames: Map<Int, String> = mapOf(
        APP_PID to "com.example.app",
        SYSTEM_PID to "com.android.systemui",
    )

    val entries: List<LogEntry> = listOf(
        entry(
            id = 1,
            millis = 0,
            pid = APP_PID,
            level = LogLevel.DEBUG,
            tag = "MainActivity",
            message = "onCreate: savedInstanceState=null",
        ),
        entry(
            id = 2,
            millis = 37,
            pid = APP_PID,
            level = LogLevel.INFO,
            tag = "MainActivity",
            message = "Cold start finished in 412 ms",
        ),
        entry(
            id = 3,
            millis = 1_250,
            pid = APP_PID,
            level = LogLevel.DEBUG,
            tag = "OkHttp",
            message = """<-- 200 https://api.example.com/v1/items {"id":7,"name":"Sample","inStock":true}""",
        ),
        entry(
            id = 4,
            millis = 1_302,
            pid = SYSTEM_PID,
            level = LogLevel.WARN,
            tag = "Choreographer",
            message = "Skipped 41 frames!  The application may be doing too much work on its main thread.",
        ),
        entry(
            id = 5,
            millis = 2_010,
            pid = APP_PID,
            level = LogLevel.ERROR,
            tag = "AndroidRuntime",
            message = "FATAL EXCEPTION: main\njava.lang.IllegalStateException: Item id must not be blank\n\tat com.example.app.ItemRepository.load(ItemRepository.kt:58)",
        ),
    )

    private fun entry(
        id: Long,
        millis: Long,
        pid: Int,
        level: LogLevel,
        tag: String,
        message: String,
    ): LogEntry {
        val epochMillis = BASE_EPOCH_MILLIS + millis
        return LogEntry(
            id = id,
            epochSeconds = epochMillis / 1_000,
            nanos = (epochMillis % 1_000 * 1_000_000).toInt(),
            pid = pid,
            tid = pid + 17,
            level = level,
            tag = tag,
            message = message,
        )
    }

    private const val BASE_EPOCH_MILLIS = 1_791_540_014_234L
}
