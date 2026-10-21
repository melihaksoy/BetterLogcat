package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.filter.LevelVisibility
import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.filter.QueryKey
import com.melihaksoy.betterlogcat.filter.QueryParser
import com.melihaksoy.betterlogcat.filter.QueryTerm
import com.melihaksoy.betterlogcat.filter.TermFold
import com.melihaksoy.betterlogcat.filter.TermFolding
import com.melihaksoy.betterlogcat.ui.logcat.model.SavedFiltersUiModel
import com.intellij.openapi.util.SystemInfo
import androidx.compose.ui.input.key.isCtrlPressed
import com.melihaksoy.betterlogcat.filter.SuggestionContext
import com.melihaksoy.betterlogcat.filter.Suggestion
import com.melihaksoy.betterlogcat.filter.QuerySuggestions
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.preview.LogcatPreviewData
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys


/** Second row: search field and level selector. A third row shows tag chips when any exist. */
@Composable
internal fun FilterBar(
    modifier: Modifier = Modifier,
    filter: LogFilter,
    isQueryValid: Boolean,
    suggestionContext: SuggestionContext,
    savedFilters: SavedFiltersUiModel,
    colors: LogColors,
    onIntent: (LogcatIntent) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 6.dp,
                    end = 6.dp,
                    bottom = 4.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            SearchField(
                modifier = Modifier.weight(1f),
                filter = filter,
                isQueryValid = isQueryValid,
                suggestionContext = suggestionContext,
                savedFilters = savedFilters,
                colors = colors,
                onIntent = onIntent,
            )
            LevelSelector(
                levels = remember(filter) { LevelVisibility.visible(filter = filter) },
                colors = colors,
                onToggle = { onIntent(LogcatIntent.ToggleLevel(level = it)) },
            )
        }
    }
}

@Composable
private fun SearchField(
    modifier: Modifier = Modifier,
    filter: LogFilter,
    isQueryValid: Boolean,
    suggestionContext: SuggestionContext,
    savedFilters: SavedFiltersUiModel,
    colors: LogColors,
    onIntent: (LogcatIntent) -> Unit,
) {
    val queryState = rememberTextFieldState(initialText = filter.query)
    val currentOnIntent by rememberUpdatedState(newValue = onIntent)
    // Last text this field sent. The field only takes the state value when the ViewModel changed it
    // (a term became a chip, or a reset), never for a plain echo, so fast typing never loses a key.
    val lastSent = remember { mutableStateOf(value = filter.query) }
    LaunchedEffect(queryState) {
        snapshotFlow { queryState.text.toString() }
            .collect { text ->
                lastSent.value = text
                currentOnIntent(LogcatIntent.ChangeQuery(query = text))
            }
    }
    LaunchedEffect(filter.query) {
        if (filter.query != lastSent.value && queryState.text.toString() != filter.query) {
            lastSent.value = filter.query
            queryState.setTextAndPlaceCursorAtEnd(text = filter.query)
        }
    }

    var isFocused by remember { mutableStateOf(value = false) }
    var isDismissed by remember { mutableStateOf(value = false) }
    var isForced by remember { mutableStateOf(value = false) }
    var isSavedOpen by remember { mutableStateOf(value = false) }
    var fieldSize by remember { mutableStateOf(value = IntSize.Zero) }
    val text = queryState.text.toString()
    val cursor = queryState.selection.end
    val folds = remember(text, cursor, isFocused) {
        TermFolding.folds(
            text = text,
            cursor = if (isFocused) cursor else -1,
        )
    }
    // Terms stay in the text, colored like chips, as in Android Studio's Logcat.
    val termColors = rememberTermColors(
        colors = colors,
        folds = folds,
    )
    val suggestions = remember(text, cursor, suggestionContext) {
        QuerySuggestions.suggest(
            text = text,
            cursor = cursor,
            context = suggestionContext,
        )
    }
    // An empty token lists the keys without picking one, so Enter still commits the text.
    var selectedIndex by remember(suggestions) { mutableStateOf(value = if (suggestions.isTokenEmpty) -1 else 0) }
    LaunchedEffect(text) { isDismissed = false }
    // Typing shows suggestions; an empty spot shows the key list only on Ctrl+Space, like Android Studio.
    val isPopupVisible = isFocused && !isDismissed && suggestions.items.isNotEmpty() && (!suggestions.isTokenEmpty || isForced)
    LaunchedEffect(suggestions.isTokenEmpty) { if (!suggestions.isTokenEmpty) isForced = false }

    fun pick(suggestion: Suggestion) {
        queryState.edit {
            replace(suggestions.tokenStart, suggestions.tokenEnd, suggestion.replacement)
            placeCursorAtEnd()
        }
    }

    Box(modifier = modifier) {
        TextField(
            state = queryState,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { fieldSize = it }
                .onFocusChanged { isFocused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.Spacebar && event.isCtrlPressed -> {
                            isForced = true
                            isDismissed = false
                            true
                        }
                        isPopupVisible && event.key == Key.DirectionDown -> {
                            selectedIndex = (selectedIndex + 1).coerceAtMost(maximumValue = suggestions.items.lastIndex)
                            true
                        }
                        isPopupVisible && event.key == Key.DirectionUp -> {
                            selectedIndex = (selectedIndex - 1).coerceAtLeast(minimumValue = -1)
                            true
                        }
                        isPopupVisible && event.key == Key.Escape -> {
                            isDismissed = true
                            isForced = false
                            true
                        }
                        isPopupVisible && selectedIndex >= 0 && (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.Tab) -> {
                            pick(suggestion = suggestions.items[selectedIndex])
                            true
                        }
                        event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                            // Terms already apply as typed; Enter only closes the suggestions.
                            isDismissed = true
                            true
                        }
                        else -> false
                    }
                },
            outline = if (isQueryValid) Outline.None else Outline.Error,
            outputTransformation = termColors,
            placeholder = {
                Text(text = BetterLogcatBundle.message("filter.search.placeholder", if (SystemInfo.isMac) "\u2303Space" else "Ctrl+Space"))
            },
            leadingIcon = {
                ToolbarIconButton(
                    modifier = Modifier.padding(end = 4.dp),
                    iconKey = AllIconsKeys.General.Filter,
                    tooltip = BetterLogcatBundle.message("filters.button"),
                    onClick = {
                        currentOnIntent(LogcatIntent.RefreshSavedFilters)
                        isSavedOpen = true
                    },
                )
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 2.dp),
                ) {
                    ToolbarToggleButton(
                        iconKey = AllIconsKeys.Actions.MatchCase,
                        tooltip = BetterLogcatBundle.message("filter.matchCase"),
                        isSelected = filter.isMatchCase,
                        onToggle = { onIntent(LogcatIntent.SetMatchCase(isMatchCase = it)) },
                    )
                    ToolbarToggleButton(
                        iconKey = AllIconsKeys.Actions.Regex,
                        tooltip = BetterLogcatBundle.message("filter.regex"),
                        isSelected = filter.isRegex,
                        onToggle = { onIntent(LogcatIntent.SetRegex(isRegex = it)) },
                    )
                }
            },
        )
        if (isSavedOpen) {
            val density = LocalDensity.current
            SavedFiltersPopup(
                savedFilters = savedFilters,
                currentQuery = filter.toQueryString(),
                width = with(density) { fieldSize.width.toDp() }.coerceAtMost(maximumValue = MAX_POPUP_WIDTH),
                offsetY = fieldSize.height + with(density) { 2.dp.roundToPx() },
                colors = colors,
                onApply = { query ->
                    isSavedOpen = false
                    currentOnIntent(LogcatIntent.ApplySavedFilter(query = query))
                },
                onToggleFavorite = { currentOnIntent(LogcatIntent.ToggleFavoriteFilter(query = it)) },
                onRemove = { currentOnIntent(LogcatIntent.RemoveFromHistory(query = it)) },
                onDismiss = { isSavedOpen = false },
            )
        }
        if (isPopupVisible) {
            val density = LocalDensity.current
            SearchSuggestionsPopup(
                suggestions = suggestions,
                selectedIndex = selectedIndex,
                width = with(density) { fieldSize.width.toDp() }.coerceAtMost(maximumValue = MAX_POPUP_WIDTH),
                offsetY = fieldSize.height + with(density) { 2.dp.roundToPx() },
                colors = colors,
                onPick = ::pick,
                onDismiss = { isDismissed = true },
            )
        }
    }
}

private val MAX_POPUP_WIDTH = 520.dp

/** V D I W E A toggles. Each level turns on and off by itself. */
@Composable
internal fun LevelSelector(
    modifier: Modifier = Modifier,
    levels: Set<LogLevel>,
    colors: LogColors,
    onToggle: (LogLevel) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        LogLevel.entries.forEach { level ->
            val isActive = level in levels
            val color = colors.level(level = level)
            val tooltipKey = if (isActive) "filter.level.tooltip.hide" else "filter.level.tooltip.show"
            Tooltip(tooltip = { Text(text = BetterLogcatBundle.message(tooltipKey, levelName(level = level))) }) {
                Box(
                    modifier = Modifier
                        .size(
                            width = 22.dp,
                            height = 22.dp,
                        )
                        .background(
                            color = if (isActive) color.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(size = 4.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = if (isActive) color.copy(alpha = 0.5f) else colors.muted.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(size = 4.dp),
                        )
                        .clickable { onToggle(level) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = level.letter.toString(),
                        color = if (isActive) color else colors.muted.copy(alpha = 0.6f),
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (isActive) TextDecoration.None else TextDecoration.LineThrough,
                    )
                }
            }
        }
    }
}

/**
 * Colors every term and shows long values short ([folds]). Styles do not move with a later replace, so
 * every replace runs first (from the end), then the styles go on at the shifted places.
 */
@Composable
private fun rememberTermColors(
    colors: LogColors,
    folds: List<TermFold>,
): OutputTransformation {
    val normal = JewelTheme.globalColors.text.normal
    return remember(colors, normal, folds) {
        OutputTransformation {
            val terms = QueryParser.termRanges(text = asCharSequence().toString())
            // Folds come from the last composition; a fold that does not sit inside its term is stale.
            val applied = terms.map { (range, _) -> folds.firstOrNull { it.range.last == range.last && it.range.first > range.first } }
            applied.asReversed().forEach { fold -> fold?.let { replace(it.range.first, it.range.last + 1, it.text) } }
            var shift = 0
            terms.forEachIndexed { index, (range, term) ->
                val start = range.first + shift
                applied[index]?.let { shift += it.text.length - (it.range.last + 1 - it.range.first) }
                val color = termColor(
                    term = term,
                    colors = colors,
                    normal = normal,
                )
                addStyle(
                    SpanStyle(
                        color = color,
                        background = color.copy(alpha = TERM_TINT),
                    ),
                    start,
                    range.last + 1 + shift,
                )
            }
        }
    }
}

private const val TERM_TINT = 0.16f

private fun termColor(
    term: QueryTerm,
    colors: LogColors,
    normal: Color,
): Color = when {
    term.isNegated -> colors.muted
    term.key == QueryKey.TAG -> colors.tag(tag = term.value)
    // A list (`level=:E,A`) takes the color of its first level.
    term.key == QueryKey.LEVEL -> term.value.substringBefore(',').let { value ->
        LogLevel.entries
            .firstOrNull { it.name.equals(value, ignoreCase = true) || it.letter.toString().equals(value, ignoreCase = true) }
            ?.let { colors.level(level = it) }
            ?: colors.error
    }
    term.key == QueryKey.STATUS -> term.value.firstOrNull()?.digitToIntOrNull()?.let { colors.httpStatus(statusCode = it * 100) } ?: normal
    term.key == QueryKey.PACKAGE -> colors.link
    else -> normal
}

private fun levelName(level: LogLevel): String = when (level) {
    LogLevel.VERBOSE -> BetterLogcatBundle.message("level.verbose")
    LogLevel.DEBUG -> BetterLogcatBundle.message("level.debug")
    LogLevel.INFO -> BetterLogcatBundle.message("level.info")
    LogLevel.WARN -> BetterLogcatBundle.message("level.warn")
    LogLevel.ERROR -> BetterLogcatBundle.message("level.error")
    LogLevel.ASSERT -> BetterLogcatBundle.message("level.assert")
}

// region Previews

private val PREVIEW_SUGGESTIONS = SuggestionContext(
    packages = listOf("com.example.pokedex"),
    tags = listOf("OkHttp", "ActivityThread"),
    processNames = listOf("com.example.pokedex"),
)

@Preview
@Composable
private fun PreviewFilterBar() {
    BetterLogcatPreviewTheme {
        FilterBar(
            filter = LogFilter(),
            isQueryValid = true,
            suggestionContext = PREVIEW_SUGGESTIONS,
            savedFilters = SavedFiltersUiModel(),
            colors = rememberLogColors(),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun PreviewFilterBarWithChips() {
    BetterLogcatPreviewTheme {
        FilterBar(
            filter = LogcatPreviewData.filteredState.filter,
            isQueryValid = true,
            suggestionContext = PREVIEW_SUGGESTIONS,
            savedFilters = SavedFiltersUiModel(),
            colors = rememberLogColors(),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun PreviewFilterBarInvalidRegex() {
    BetterLogcatPreviewTheme {
        FilterBar(
            filter = LogFilter(
                query = "user(id=",
                isRegex = true,
            ),
            isQueryValid = false,
            suggestionContext = PREVIEW_SUGGESTIONS,
            savedFilters = SavedFiltersUiModel(),
            colors = rememberLogColors(),
            onIntent = {},
        )
    }
}

// endregion
