package com.melihaksoy.betterlogcat.model

/** How the Time column shows a timestamp. [widthDp] fits the longest value in the editor font. */
enum class TimeFormat(val widthDp: Int) {
    /** 12:00:01.037 */
    TIME(widthDp = 112),

    /** 10-09 12:00:01.037 */
    DATE_TIME(widthDp = 162),

    /** 2026-10-09 12:00:01.037 */
    FULL(widthDp = 204),

    /** 1791540001.037 */
    EPOCH(widthDp = 128),

    /** +0.037s, time since the row above */
    DELTA(widthDp = 92),
}
