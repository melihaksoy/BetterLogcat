package com.melihaksoy.betterlogcat.ui.logcat

sealed interface LogcatEvent {
    data class ShowExportDialog(val defaultFileName: String) : LogcatEvent
    data class ShowStatus(val message: String) : LogcatEvent
    data object ShowMappingChooser : LogcatEvent
}
