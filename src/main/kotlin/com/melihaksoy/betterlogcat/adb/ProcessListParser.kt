package com.melihaksoy.betterlogcat.adb

/** One running process. [isApp] is true for app processes (user `u0_a123`, or `app_123` on old devices). */
data class ProcessInfo(
    val name: String,
    val isApp: Boolean,
)

/** Parses `ps` output. Works with toybox `ps -A -o PID,USER,NAME` and old toolbox `ps`. */
object ProcessListParser {

    private val whitespace = Regex("\\s+")
    private val appUser = Regex("u\\d+_[ai]\\d+|app_\\d+")

    fun parse(output: String): Map<Int, ProcessInfo> {
        val lines = output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty()) return emptyMap()
        val header = lines.first().split(whitespace)
        val pidIndex = header.indexOf("PID")
        val userIndex = header.indexOf("USER")
        if (pidIndex < 0) return emptyMap()

        return lines.drop(1)
            .mapNotNull { line ->
                val columns = line.split(whitespace)
                val pid = columns.getOrNull(pidIndex)?.toIntOrNull() ?: return@mapNotNull null
                val name = columns.last()
                if (columns.size <= pidIndex + 1 || name.isEmpty()) return@mapNotNull null
                val user = if (userIndex >= 0) columns.getOrNull(userIndex).orEmpty() else ""
                pid to ProcessInfo(
                    name = name,
                    isApp = appUser.matches(input = user),
                )
            }
            .toMap()
    }
}
