package com.melihaksoy.betterlogcat.ui.logcat.model

import androidx.compose.runtime.Immutable
import com.melihaksoy.betterlogcat.model.FrameSkip
import com.melihaksoy.betterlogcat.model.ProblemSession

/**
 * What the Problems panel shows: this pane's live session, the saved sessions before it (newest
 * first), and the frame drops of the live session.
 */
@Immutable
data class ProblemsUiModel(
    val current: ProblemSession? = null,
    val history: List<ProblemSession> = emptyList(),
    val frameSkips: List<FrameSkip> = emptyList(),
    /** Problems the user ignored; they are not recorded until reset. */
    val ignoredCount: Int = 0,
) {
    val currentCount: Int
        get() = current?.problems?.size ?: 0
}
