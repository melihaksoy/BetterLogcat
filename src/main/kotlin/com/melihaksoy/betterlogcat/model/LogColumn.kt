package com.melihaksoy.betterlogcat.model

enum class LogColumn(
    val isResizable: Boolean,
    val defaultWidthDp: Int,
) {
    TIME(isResizable = true, defaultWidthDp = 112),
    PROCESS(isResizable = true, defaultWidthDp = 104),
    PACKAGE(isResizable = true, defaultWidthDp = 180),
    TAG(isResizable = true, defaultWidthDp = 160),
    LEVEL(isResizable = false, defaultWidthDp = 24),
}
