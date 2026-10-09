package com.melihaksoy.betterlogcat.ui.logcat.model

import androidx.compose.runtime.Immutable

/** A cell that a double click turns into a read-only text box. */
enum class SelectableCell {
    NONE,
    TIME,
    PROCESS,
    PACKAGE,
    TAG,
    MESSAGE,
}

/** The cell in text selection mode. [entryId] is [SelectionUiModel.NO_ROW] when none. */
@Immutable
data class TextSelectionUiModel(
    val entryId: Long = SelectionUiModel.NO_ROW,
    val cell: SelectableCell = SelectableCell.NONE,
) {
    val isActive: Boolean
        get() = entryId != SelectionUiModel.NO_ROW && cell != SelectableCell.NONE

    fun cellFor(id: Long): SelectableCell = if (id == entryId) cell else SelectableCell.NONE
}
