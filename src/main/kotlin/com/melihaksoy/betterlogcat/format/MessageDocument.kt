package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.JsonContent
import com.melihaksoy.betterlogcat.model.LogEntry

/** A message as a document for an editor tab or the diff viewer: JSON bodies pretty printed. */
object MessageDocument {

    fun text(entry: LogEntry): String {
        val json = entry.json as? JsonContent.Found ?: return entry.message
        if (!json.main.isComplete) return entry.message
        return buildString {
            val prefix = json.prefix.trimEnd()
            if (prefix.isNotEmpty()) append(prefix).append('\n')
            append(json.pretty)
            val suffix = json.suffix.trim()
            if (suffix.isNotEmpty()) append('\n').append(suffix)
        }
    }

    /** True when the document is only JSON, so an editor can highlight it as JSON. */
    fun isPureJson(entry: LogEntry): Boolean {
        val json = entry.json as? JsonContent.Found ?: return false
        return json.main.isComplete && json.prefix.isBlank() && json.suffix.isBlank()
    }
}
