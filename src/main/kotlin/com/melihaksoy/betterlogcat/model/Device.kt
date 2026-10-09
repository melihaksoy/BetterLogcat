package com.melihaksoy.betterlogcat.model

import androidx.compose.runtime.Immutable

enum class DeviceState {
    ONLINE,
    OFFLINE,
    UNAUTHORIZED,
    OTHER,
}

@Immutable
data class Device(
    val serial: String,
    val state: DeviceState,
    val name: String,
    val androidVersion: String,
    val apiLevel: String,
) {
    val isEmulator: Boolean
        get() = serial.startsWith("emulator-")
}
