package com.melihaksoy.betterlogcat.ui.logcat.model

import androidx.compose.runtime.Immutable

/**
 * Favorite and recent filters, as query strings like `package:mine -tag:art timeout`.
 * [counts] is how many entries in the buffer each one matches, capped at [COUNT_LIMIT].
 */
@Immutable
data class SavedFiltersUiModel(
    val favorites: List<String> = emptyList(),
    val history: List<String> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
) {
    companion object {
        const val COUNT_LIMIT = 100
    }
}
