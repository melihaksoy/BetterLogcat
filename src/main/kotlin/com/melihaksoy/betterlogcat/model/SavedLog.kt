package com.melihaksoy.betterlogcat.model

/** The end of one device's log, kept for the next IDE session. */
class SavedLog(
    val serial: String,
    val deviceName: String,
    /** Oldest first. */
    val entries: List<LogEntry>,
    val processNames: Map<Int, String>,
)
