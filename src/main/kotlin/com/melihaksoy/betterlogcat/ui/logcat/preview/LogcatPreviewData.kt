package com.melihaksoy.betterlogcat.ui.logcat.preview

import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.filter.QueryKey
import com.melihaksoy.betterlogcat.filter.QueryTerm
import com.melihaksoy.betterlogcat.model.Device
import com.melihaksoy.betterlogcat.model.DeviceState
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.ui.logcat.LogcatState
import com.melihaksoy.betterlogcat.ui.logcat.model.LogcatContentUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.SelectionUiModel

/** Realistic sample data for previews only. */
internal object LogcatPreviewData {

    private const val APP_PID = 14_302
    private const val ADBD_PID = 611

    val pixel = Device(
        serial = "38211FDJH00A4K",
        state = DeviceState.ONLINE,
        name = "Google Pixel 9",
        androidVersion = "16",
        apiLevel = "36",
    )

    val emulator = Device(
        serial = "emulator-5554",
        state = DeviceState.ONLINE,
        name = "Medium Phone API 35",
        androidVersion = "15",
        apiLevel = "35",
    )

    val entries: List<LogEntry> = listOf(
        entry(
            id = 1,
            level = LogLevel.DEBUG,
            tag = "ActivityThread",
            message = "handleBindApplication: com.example.pokedex",
            pid = APP_PID,
        ),
        entry(
            id = 2,
            level = LogLevel.INFO,
            tag = "PokedexApp",
            message = "Cold start finished in 412 ms",
            pid = APP_PID,
        ),
        entry(
            id = 3,
            level = LogLevel.DEBUG,
            tag = "OkHttp",
            message = """<-- 200 https://pokeapi.co/api/v2/pokemon/25 {"id":25,"name":"pikachu","height":4,"weight":60,"legendary":false}""",
            pid = APP_PID,
        ),
        entry(
            id = 4,
            level = LogLevel.WARN,
            tag = "Choreographer",
            message = "Skipped 41 frames!  The application may be doing too much work on its main thread.",
            pid = APP_PID,
        ),
        entry(
            id = 5,
            level = LogLevel.ERROR,
            tag = "AndroidRuntime",
            message = """
                FATAL EXCEPTION: main
                Process: com.example.pokedex, PID: 14302
                java.lang.IllegalStateException: Pokemon id must not be blank
                	at com.example.pokedex.data.PokemonRepository.load(PokemonRepository.kt:58)
                	at com.example.pokedex.ui.PokemonViewModel.refresh(PokemonViewModel.kt:31)
                	at android.os.Handler.dispatchMessage(Handler.java:106)
            """.trimIndent(),
            pid = APP_PID,
        ),
        entry(
            id = 6,
            level = LogLevel.VERBOSE,
            tag = "ViewRootImpl",
            message = "dispatchDetachedFromWindow",
            pid = 1_822,
        ),
    )

    val tagCounts: Map<String, Int> = mapOf(
        "ActivityThread" to 41,
        "PokedexApp" to 18,
        "OkHttp" to 233,
        "Choreographer" to 12,
        "AndroidRuntime" to 1,
        "ViewRootImpl" to 77,
        "art" to 412,
        "adbd" to 96,
    )

    val logsState = LogcatState(
        content = LogcatContentUiModel.Logs(isDeviceConnected = true),
        devices = listOf(pixel, emulator),
        selectedSerial = pixel.serial,
        entries = entries,
        totalCount = 1_284,
        processNames = mapOf(
            APP_PID to "com.example.pokedex",
            1_822 to "com.android.systemui",
        ),
        packages = listOf("com.android.systemui", "com.example.pokedex"),
        projectAppIds = setOf("com.example.pokedex"),
        tagCounts = tagCounts,
        // Which processes logged each tag: the Tags panel splits app tags from the rest with it, and
        // keeps only app tags under package:mine. ART logs inside every app; adbd is its own process.
        tagPids = mapOf(
            "ActivityThread" to setOf(APP_PID, 1_822),
            "PokedexApp" to setOf(APP_PID),
            "OkHttp" to setOf(APP_PID),
            "Choreographer" to setOf(APP_PID),
            "AndroidRuntime" to setOf(APP_PID),
            "ViewRootImpl" to setOf(APP_PID, 1_822),
            "art" to setOf(APP_PID, 1_822),
            "adbd" to setOf(ADBD_PID),
        ),
        selection = SelectionUiModel(
            anchorId = 4,
            leadId = 5,
            selectedIds = setOf(4L, 5L),
        ),
    )

    val filteredState = logsState.copy(
        filter = LogFilter(
            levels = setOf(LogLevel.WARN, LogLevel.ERROR, LogLevel.ASSERT),
            query = "pokemon",
            terms = listOf(
                QueryTerm(
                    key = QueryKey.PACKAGE,
                    value = "com.example.pokedex",
                ),
                QueryTerm(
                    key = QueryKey.TAG,
                    value = "art",
                    isNegated = true,
                ),
            ),
        ),
    )

    private fun entry(
        id: Long,
        level: LogLevel,
        tag: String,
        message: String,
        pid: Int,
    ) = LogEntry(
        id = id,
        epochSeconds = 1_791_540_000L + id,
        nanos = (id * 37_000_000L % 1_000_000_000L).toInt(),
        pid = pid,
        tid = pid + 28,
        level = level,
        tag = tag,
        message = message,
    )
}
