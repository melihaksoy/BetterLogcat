package com.melihaksoy.betterlogcat.ui.logcat.model

import androidx.compose.runtime.Immutable

/** Lines loaded from the previous IDE session: every entry up to [lastId]. [deviceName] is empty when unknown. */
@Immutable
data class PreviousLogUiModel(
    val lastId: Long,
    val deviceName: String,
)
