package com.melihaksoy.betterlogcat.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/** An app process starting or ending, read from ActivityManager's own log lines. */
@Stable
sealed interface ProcessEvent {
    val packageName: String
    val pid: Int

    @Immutable
    data class Started(
        override val packageName: String,
        override val pid: Int,
    ) : ProcessEvent

    @Immutable
    data class Died(
        override val packageName: String,
        override val pid: Int,
    ) : ProcessEvent

    companion object {
        private const val TAG = "ActivityManager"
        private val startRegex = Regex(pattern = """^Start proc (\d+):([\w.]+)(?::[\w.]+)?/""")
        private val diedRegex = Regex(pattern = """^Process ([\w.]+)(?::[\w.]+)? \(pid (\d+)\) has died""")
        private val killRegex = Regex(pattern = """^Killing (\d+):([\w.]+)(?::[\w.]+)?/""")

        fun parse(
            tag: String,
            message: String,
        ): ProcessEvent? {
            if (tag != TAG) return null
            startRegex.find(input = message)?.let { match ->
                return Started(
                    packageName = match.groupValues[2],
                    pid = match.groupValues[1].toInt(),
                )
            }
            diedRegex.find(input = message)?.let { match ->
                return Died(
                    packageName = match.groupValues[1],
                    pid = match.groupValues[2].toInt(),
                )
            }
            killRegex.find(input = message)?.let { match ->
                return Died(
                    packageName = match.groupValues[2],
                    pid = match.groupValues[1].toInt(),
                )
            }
            return null
        }
    }
}
