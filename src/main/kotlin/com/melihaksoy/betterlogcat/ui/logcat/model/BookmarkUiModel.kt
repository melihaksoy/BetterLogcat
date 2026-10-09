package com.melihaksoy.betterlogcat.ui.logcat.model

import androidx.compose.runtime.Immutable
import com.melihaksoy.betterlogcat.model.Bookmark

/** A bookmark and the id of its line in this pane's log, or [NOT_IN_LOG] after Clear, a trim or a restart. */
@Immutable
data class BookmarkUiModel(
    val bookmark: Bookmark,
    val liveId: Long = NOT_IN_LOG,
) {
    val isInLog: Boolean
        get() = liveId != NOT_IN_LOG

    companion object {
        const val NOT_IN_LOG = -1L
    }
}
