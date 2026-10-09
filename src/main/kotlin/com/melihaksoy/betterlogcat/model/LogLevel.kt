package com.melihaksoy.betterlogcat.model

enum class LogLevel(
    val priority: Int,
    val letter: Char,
) {
    VERBOSE(priority = 2, letter = 'V'),
    DEBUG(priority = 3, letter = 'D'),
    INFO(priority = 4, letter = 'I'),
    WARN(priority = 5, letter = 'W'),
    ERROR(priority = 6, letter = 'E'),
    ASSERT(priority = 7, letter = 'A'),
    ;

    companion object {
        fun fromPriority(priority: Int): LogLevel = when {
            priority >= ASSERT.priority -> ASSERT
            else -> entries.firstOrNull { it.priority == priority } ?: VERBOSE
        }
    }
}
