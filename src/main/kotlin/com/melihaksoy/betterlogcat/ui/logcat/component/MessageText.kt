package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.filter.LogFilterMatcher
import com.melihaksoy.betterlogcat.format.JsonHighlighter
import com.melihaksoy.betterlogcat.format.StackFrame
import com.melihaksoy.betterlogcat.format.StackFrameParser
import com.melihaksoy.betterlogcat.format.UrlFinder
import com.melihaksoy.betterlogcat.model.HighlightRule
import com.melihaksoy.betterlogcat.model.JsonContent
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import java.util.Locale
import java.util.regex.PatternSyntaxException

/** Search highlight settings. A data class, so rows only rebuild text when the query changes. */
@Immutable
data class SearchHighlighter(
    val query: String,
    val isRegex: Boolean,
    val isMatchCase: Boolean,
) {
    private val matcher by lazy(LazyThreadSafetyMode.NONE) {
        LogFilterMatcher(
            filter = LogFilter(
                query = query,
                isRegex = isRegex,
                isMatchCase = isMatchCase,
            ),
            processNames = emptyMap(),
        )
    }

    fun ranges(text: String): List<IntRange> = if (query.isEmpty()) emptyList() else matcher.highlights(text = text)

    companion object {
        val None = SearchHighlighter(
            query = "",
            isRegex = false,
            isMatchCase = false,
        )

        fun from(filter: LogFilter) = SearchHighlighter(
            query = filter.effective().query,
            isRegex = filter.isRegex,
            isMatchCase = filter.isMatchCase,
        )
    }
}

/** The user's keyword highlights, compiled once. A data class on the rules, so rows rebuild only when they change. */
@Immutable
data class KeywordHighlighter(val rules: List<HighlightRule>) {

    private val compiled: List<Pair<Regex, Color>> by lazy(LazyThreadSafetyMode.NONE) {
        rules.mapNotNull { rule ->
            val regex = try {
                if (rule.isRegex) Regex(pattern = rule.pattern) else Regex(pattern = Regex.escape(rule.pattern), option = RegexOption.IGNORE_CASE)
            } catch (_: PatternSyntaxException) {
                null
            }
            regex?.let { it to Color(rule.colorArgb) }
        }
    }

    fun apply(
        builder: AnnotatedString.Builder,
        text: String,
        offset: Int,
    ) {
        if (rules.isEmpty()) return
        compiled.forEach { (regex, color) ->
            regex.findAll(input = text).take(MAX_KEYWORD_MATCHES).forEach { match ->
                if (match.range.isEmpty()) return@forEach
                builder.addStyle(
                    style = SpanStyle(
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        background = color.copy(alpha = KEYWORD_TINT),
                    ),
                    start = offset + match.range.first,
                    end = offset + match.range.last + 1,
                )
            }
        }
    }

    companion object {
        val None = KeywordHighlighter(rules = emptyList())
        private const val MAX_KEYWORD_MATCHES = 64
        private const val KEYWORD_TINT = 0.16f
    }
}

/** Labels shown inside message text. Passed in because they come from the message bundle. */
@Immutable
data class MessageLabels(
    val showAll: (Int, String) -> String,
)

private const val JSON_PREVIEW_CHARS = 110
private const val TEXT_PREVIEW_CHARS = 200
private const val URL_SEPARATOR_CHARS = 5

internal fun buildMessageText(
    entry: LogEntry,
    isExpanded: Boolean,
    prettyJson: Boolean,
    isFullyShown: Boolean,
    textColor: Color,
    colors: LogColors,
    highlighter: SearchHighlighter,
    keywords: KeywordHighlighter,
    labels: MessageLabels,
    onOpenFrame: (StackFrame) -> Unit,
    onOpenUrl: (String) -> Unit,
    onShowAll: () -> Unit,
): AnnotatedString {
    val text = buildAnnotatedString {
        val append = { part: String ->
            appendMessage(
                text = part,
                textColor = textColor,
                colors = colors,
                highlighter = highlighter,
                keywords = keywords,
                onOpenFrame = onOpenFrame,
                onOpenUrl = onOpenUrl,
            )
        }
        val appendJsonPart = { json: String ->
            appendJson(
                json = json,
                textColor = textColor,
                colors = colors,
                highlighter = highlighter,
                keywords = keywords,
                onOpenUrl = onOpenUrl,
            )
        }
        val json = entry.json
        when {
            isExpanded && json is JsonContent.Found -> appendExpandedJson(
                message = entry.message,
                json = json,
                prettyJson = prettyJson,
                append = append,
                appendJsonPart = appendJsonPart,
            )
            isExpanded || !entry.isExpandable -> append(entry.message)
            else -> appendCollapsed(
                entry = entry,
                colors = colors,
                labels = labels,
                append = append,
                appendJsonPart = appendJsonPart,
                onOpenUrl = onOpenUrl,
            )
        }
    }
    return if (isExpanded && !isFullyShown) {
        capped(
            text = text,
            colors = colors,
            labels = labels,
            onShowAll = onShowAll,
        )
    } else {
        text
    }
}

/**
 * Every JSON part in place. The main part and parts that start a line are pretty printed on their
 * own lines; small inline parts (header values) stay on their line. All parts get colors.
 */
private fun AnnotatedString.Builder.appendExpandedJson(
    message: String,
    json: JsonContent.Found,
    prettyJson: Boolean,
    append: (String) -> Unit,
    appendJsonPart: (String) -> Unit,
) {
    var cursor = 0
    var needsBreak = false
    json.blocks.sortedBy { it.start }.forEach { block ->
        // Incomplete parts have a lenient pretty form too, so a cut-off body is still indented.
        val isPretty = prettyJson && !block.isInline
        var before = message.substring(cursor, block.start)
        if (needsBreak && before.isNotEmpty() && !before.startsWith('\n')) before = "\n" + before
        if (isPretty) {
            val trimmed = before.trimEnd(' ', '\t')
            append(trimmed)
            if (length > 0 && !(trimmed.isEmpty() && needsBreak) && !trimmed.endsWith('\n')) append("\n")
            appendJsonPart(block.pretty)
        } else {
            append(before)
            appendJsonPart(block.raw)
        }
        needsBreak = isPretty
        cursor = block.end
    }
    var after = message.substring(cursor)
    if (needsBreak && after.isNotEmpty() && !after.startsWith('\n')) after = "\n" + after
    append(after)
}

/**
 * One compact line: the start of the first line, the first web address when the first line has none
 * (so a response shows which request it answers), a preview of the main JSON part, then what is hidden.
 */
private fun AnnotatedString.Builder.appendCollapsed(
    entry: LogEntry,
    colors: LogColors,
    labels: MessageLabels,
    append: (String) -> Unit,
    appendJsonPart: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val firstLine = entry.lines.first()
    val json = entry.json as? JsonContent.Found
    val firstLineText = if (json != null && json.main.start < firstLine.length) firstLine.substring(0, json.main.start) else firstLine
    append(if (firstLineText.length > TEXT_PREVIEW_CHARS) firstLineText.take(TEXT_PREVIEW_CHARS) + "…" else firstLineText.trimEnd())
    if (!firstLine.contains("http")) {
        UrlFinder.find(text = entry.message.take(URL_SEARCH_CHARS)).firstOrNull()?.let { match ->
            appendMuted(
                text = "  ·  ",
                colors = colors,
            )
            append(entry.message.substring(match.start, match.end))
        }
    }
    if (json != null) {
        append(" ")
        val preview = json.main.minified
        appendJsonPart(if (preview.length > JSON_PREVIEW_CHARS) preview.take(JSON_PREVIEW_CHARS) + "…" else preview)
    }
}

/** Characters in the one-line view [appendCollapsed] builds for [entry], for the sideways scroll width. */
internal fun collapsedLength(entry: LogEntry): Int {
    val firstLine = entry.lines.first()
    val json = entry.json as? JsonContent.Found
    val firstLineText = if (json != null && json.main.start < firstLine.length) firstLine.substring(0, json.main.start) else firstLine
    var length = if (firstLineText.length > TEXT_PREVIEW_CHARS) TEXT_PREVIEW_CHARS + 1 else firstLineText.trimEnd().length
    if (!firstLine.contains("http")) {
        UrlFinder.find(text = entry.message.take(URL_SEARCH_CHARS)).firstOrNull()?.let { match -> length += URL_SEPARATOR_CHARS + match.end - match.start }
    }
    if (json != null) length += 1 + minOf(json.main.minified.length, JSON_PREVIEW_CHARS + 1)
    return length
}

/**
 * Laying out thousands of lines makes expanding slow, so an expanded message shows the first
 * [MAX_EXPANDED_LINES] lines (or [MAX_EXPANDED_CHARS] chars), then a "Show all" link. Copy still copies all.
 */
private fun capped(
    text: AnnotatedString,
    colors: LogColors,
    labels: MessageLabels,
    onShowAll: () -> Unit,
): AnnotatedString {
    var lines = 0
    var cut = -1
    for (index in text.indices) {
        if (text[index] == '\n' && ++lines >= MAX_EXPANDED_LINES) {
            cut = index
            break
        }
        if (index >= MAX_EXPANDED_CHARS) {
            cut = index
            break
        }
    }
    if (cut < 0) return text
    val totalLines = text.count { it == '\n' } + 1
    return buildAnnotatedString {
        append(text.subSequence(0, cut))
        append("\n")
        withLink(
            link = LinkAnnotation.Clickable(
                tag = "show-all",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = colors.link,
                        fontStyle = FontStyle.Italic,
                    ),
                ),
                linkInteractionListener = { onShowAll() },
            ),
        ) {
            append(labels.showAll(totalLines, sizeLabel(chars = text.length)))
        }
    }
}

private const val MAX_EXPANDED_LINES = 2_000
private const val MAX_EXPANDED_CHARS = 300_000
private const val URL_SEARCH_CHARS = 2_000

private fun AnnotatedString.Builder.appendJson(
    json: String,
    textColor: Color,
    colors: LogColors,
    highlighter: SearchHighlighter,
    keywords: KeywordHighlighter,
    onOpenUrl: (String) -> Unit,
) {
    val start = length
    pushStyle(style = SpanStyle(color = textColor))
    append(json)
    pop()
    keywords.apply(
        builder = this,
        text = json,
        offset = start,
    )
    JsonHighlighter.tokens(json = json).forEach { token ->
        addStyle(
            style = SpanStyle(color = colors.json(kind = token.kind)),
            start = start + token.start,
            end = start + token.end,
        )
    }
    addUrlLinks(
        text = json,
        offset = start,
        colors = colors,
        onOpenUrl = onOpenUrl,
    )
    highlighter.ranges(text = json).forEach { range ->
        addStyle(
            style = SpanStyle(background = colors.searchHighlight),
            start = start + range.first,
            end = start + range.last + 1,
        )
    }
}

private fun AnnotatedString.Builder.appendMuted(
    text: String,
    colors: LogColors,
) {
    pushStyle(
        style = SpanStyle(
            color = colors.muted,
            fontStyle = FontStyle.Italic,
        ),
    )
    append(text)
    pop()
}

private fun sizeLabel(chars: Int): String = if (chars < 1_024) "$chars B" else String.format(Locale.ROOT, "%.1f KB", chars / 1_024.0)

/** Makes web links clickable. Underlined in the link color, opened in the browser. */
private fun AnnotatedString.Builder.addUrlLinks(
    text: String,
    offset: Int,
    colors: LogColors,
    onOpenUrl: (String) -> Unit,
) {
    UrlFinder.find(text = text).forEach { match ->
        addLink(
            clickable = LinkAnnotation.Clickable(
                tag = "url",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = colors.link,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
                linkInteractionListener = { onOpenUrl(match.url) },
            ),
            start = offset + match.start,
            end = offset + match.end,
        )
    }
}

private fun AnnotatedString.Builder.appendMessage(
    text: String,
    textColor: Color,
    colors: LogColors,
    highlighter: SearchHighlighter,
    keywords: KeywordHighlighter,
    onOpenFrame: (StackFrame) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    // Compose draws a tab as one narrow space, so stack traces lose their indent.
    val display = text.replace("\t", TAB_SPACES)
    val start = length
    pushStyle(style = SpanStyle(color = textColor))
    append(display)
    pop()
    StackFrameParser.findFrames(text = display).forEach { frame ->
        addLink(
            clickable = LinkAnnotation.Clickable(
                tag = "frame",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = colors.link,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
                linkInteractionListener = { onOpenFrame(frame) },
            ),
            start = start + frame.start,
            end = start + frame.end,
        )
    }
    keywords.apply(
        builder = this,
        text = display,
        offset = start,
    )
    addUrlLinks(
        text = display,
        offset = start,
        colors = colors,
        onOpenUrl = onOpenUrl,
    )
    highlighter.ranges(text = display).forEach { range ->
        addStyle(
            style = SpanStyle(background = colors.searchHighlight),
            start = start + range.first,
            end = start + range.last + 1,
        )
    }
}

private const val TAB_SPACES = "    "
