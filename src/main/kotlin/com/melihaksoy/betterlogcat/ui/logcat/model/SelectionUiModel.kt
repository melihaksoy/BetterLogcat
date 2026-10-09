package com.melihaksoy.betterlogcat.ui.logcat.model

import androidx.compose.runtime.Immutable

/**
 * Row selection. [anchorId] is where a Shift range starts. [leadId] is the row the keyboard moves.
 * [NO_ROW] means no row.
 */
@Immutable
data class SelectionUiModel(
    val anchorId: Long = NO_ROW,
    val leadId: Long = NO_ROW,
    val selectedIds: Set<Long> = emptySet(),
) {
    companion object {
        const val NO_ROW = -1L
    }
}
