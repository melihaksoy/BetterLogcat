package com.melihaksoy.betterlogcat.ui.logcat.model

/** On/off view settings. Each one maps to a field of [ViewOptionsUiModel]. */
enum class ViewToggle {
    SOFT_WRAP,
    COMPACT_ROWS,
    PRETTY_JSON,
    INCLUDE_THREAD_ID,
    SHOW_REPEATED_TAGS,
    SHOW_REPEATED_PACKAGES,
    COLORIZE_TAGS,
    COPY_MESSAGE_ONLY,
    EXPAND_JSON,
}
