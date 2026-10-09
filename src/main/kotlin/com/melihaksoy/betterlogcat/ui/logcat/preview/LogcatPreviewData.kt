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
            message = "handleBindApplication: com.vintagegarage.app",
            pid = APP_PID,
        ),
        entry(
            id = 2,
            level = LogLevel.INFO,
            tag = "GarageApp",
            message = "Cold start finished in 412 ms",
            pid = APP_PID,
        ),
        entry(
            id = 3,
            level = LogLevel.DEBUG,
            tag = "OkHttp",
            message = """<-- 200 https://api.vintagegarage.app/v2/listings/88213 {"id":88213,"make":"Jaguar","model":"E-Type","year":1967,"sold":false}""",
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
                Process: com.vintagegarage.app, PID: 14302
                java.lang.IllegalStateException: Listing id must not be blank
                	at com.vintagegarage.listing.ListingRepository.load(ListingRepository.kt:58)
                	at com.vintagegarage.listing.ListingViewModel.refresh(ListingViewModel.kt:31)
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
        "GarageApp" to 18,
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
            APP_PID to "com.vintagegarage.app",
            1_822 to "com.android.systemui",
        ),
        packages = listOf("com.android.systemui", "com.vintagegarage.app"),
        projectAppIds = setOf("com.vintagegarage.app"),
        tagCounts = tagCounts,
        selection = SelectionUiModel(
            anchorId = 4,
            leadId = 5,
            selectedIds = setOf(4L, 5L),
        ),
    )

    val filteredState = logsState.copy(
        filter = LogFilter(
            levels = setOf(LogLevel.WARN, LogLevel.ERROR, LogLevel.ASSERT),
            query = "listing",
            terms = listOf(
                QueryTerm(
                    key = QueryKey.PACKAGE,
                    value = "com.vintagegarage.app",
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
