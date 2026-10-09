package com.melihaksoy.betterlogcat.media

import java.io.File

data class RecordOptions(
    val bitRateMbps: Int,
    val scalePercent: Int,
    val showTaps: Boolean,
    val useEmulatorRecording: Boolean,
    val directory: File,
)
