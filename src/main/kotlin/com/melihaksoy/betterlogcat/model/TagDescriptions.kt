package com.melihaksoy.betterlogcat.model

import java.util.Properties

/**
 * One-line descriptions of common Android tags, shown when the pointer rests on a tag. Built-in ones
 * come from `tags/TagDescriptions.properties`; the user's own notes win over them. Most tags have none.
 */
object TagDescriptions {

    private const val RESOURCE = "/tags/TagDescriptions.properties"

    val builtIn: Map<String, String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val properties = Properties()
        TagDescriptions::class.java.getResourceAsStream(RESOURCE)?.reader(charset = Charsets.UTF_8)?.use { properties.load(it) }
        properties.stringPropertyNames().associateWith { properties.getProperty(it).trim() }.filterValues { it.isNotEmpty() }
    }

    /** The user's note for [tag], else the built-in description, else null. */
    fun describe(
        tag: String,
        userNotes: Map<String, String>,
    ): String? = userNotes[tag]?.takeIf { it.isNotBlank() } ?: builtIn[tag]
}
