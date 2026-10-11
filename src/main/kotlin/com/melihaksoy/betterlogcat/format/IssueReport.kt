package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.Device
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.ProblemDescriptions
import com.melihaksoy.betterlogcat.model.ProblemKind

/** The installed version of an app, from `dumpsys package`. */
data class AppVersion(
    val name: String,
    val code: String,
)

/** A crash or exception as Markdown for an issue tracker: what, app version, device, time, then the trace. */
object IssueReport {

    fun markdown(
        entry: LogEntry,
        packageName: String,
        version: AppVersion?,
        device: Device?,
    ): String {
        val kind = entry.problem ?: ProblemKind.EXCEPTION
        val description = ProblemDescriptions.describe(
            entry = entry,
            kind = kind,
        )
        return buildString {
            append("### ").append(kindLabel(kind = kind)).append(": ").append(description.title).append('\n')
            if (description.detail.isNotEmpty()) append('\n').append(description.detail).append('\n')
            append('\n')
            if (packageName.isNotEmpty()) {
                append("- **App:** `").append(packageName).append('`')
                version?.let { append(' ').append(it.name).append(" (").append(it.code).append(')') }
                append('\n')
            }
            device?.let {
                append("- **Device:** ").append(it.name)
                if (it.androidVersion.isNotEmpty()) append(", Android ").append(it.androidVersion).append(" (API ").append(it.apiLevel).append(')')
                append('\n')
            }
            append("- **Time:** ").append(LogTimeFormatter.dateTime(epochSeconds = entry.epochSeconds, nanos = entry.nanos)).append('\n')
            append("- **Tag:** `").append(entry.tag).append("`\n")
            append("\n```\n").append(entry.message.trimEnd()).append("\n```\n")
        }
    }

    /** Reads `versionName=1.2.3` and `versionCode=45 minSdk=…` from `dumpsys package <app>`. */
    fun parseVersion(dumpsys: String): AppVersion? {
        val name = VERSION_NAME.find(input = dumpsys)?.groupValues?.get(1) ?: return null
        val code = VERSION_CODE.find(input = dumpsys)?.groupValues?.get(1).orEmpty()
        return AppVersion(
            name = name,
            code = code,
        )
    }

    private fun kindLabel(kind: ProblemKind): String = when (kind) {
        ProblemKind.CRASH -> "Crash"
        ProblemKind.ANR -> "ANR"
        ProblemKind.EXCEPTION -> "Exception"
        ProblemKind.STRICT_MODE -> "StrictMode violation"
        ProblemKind.WARNING -> "Warning"
    }

    private val VERSION_NAME = Regex(pattern = """versionName=(\S+)""")
    private val VERSION_CODE = Regex(pattern = """versionCode=(\d+)""")
}
