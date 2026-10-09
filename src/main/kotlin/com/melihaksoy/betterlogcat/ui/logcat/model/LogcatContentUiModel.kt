package com.melihaksoy.betterlogcat.ui.logcat.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.melihaksoy.betterlogcat.model.DeviceState

/** What the main area of the tool window shows. */
@Stable
sealed interface LogcatContentUiModel {

    @Immutable
    data object Loading : LogcatContentUiModel

    @Immutable
    data object AdbNotFound : LogcatContentUiModel

    @Immutable
    data object NoDevices : LogcatContentUiModel

    @Immutable
    data class DeviceNotReady(
        val deviceName: String,
        val state: DeviceState,
    ) : LogcatContentUiModel

    @Immutable
    data class Logs(val isDeviceConnected: Boolean) : LogcatContentUiModel
}
