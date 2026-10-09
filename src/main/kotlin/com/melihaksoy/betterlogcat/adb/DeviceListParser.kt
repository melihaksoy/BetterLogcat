package com.melihaksoy.betterlogcat.adb

import com.melihaksoy.betterlogcat.model.DeviceState

/** One line of `adb devices -l`. */
data class DeviceListing(
    val serial: String,
    val state: DeviceState,
    val model: String,
)

object DeviceListParser {

    fun parse(output: String): List<DeviceListing> = output.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("List of devices") && !it.startsWith("*") }
        .mapNotNull { line ->
            val parts = line.split(Regex("\\s+"))
            if (parts.size < 2) return@mapNotNull null
            val properties = parts.drop(2)
                .mapNotNull { part ->
                    val separator = part.indexOf(':')
                    if (separator <= 0) null else part.substring(0, separator) to part.substring(separator + 1)
                }
                .toMap()
            DeviceListing(
                serial = parts[0],
                state = parseState(value = parts[1]),
                model = properties["model"].orEmpty().replace('_', ' '),
            )
        }
        .toList()

    private fun parseState(value: String): DeviceState = when (value) {
        "device" -> DeviceState.ONLINE
        "offline" -> DeviceState.OFFLINE
        "unauthorized" -> DeviceState.UNAUTHORIZED
        else -> DeviceState.OTHER
    }
}
