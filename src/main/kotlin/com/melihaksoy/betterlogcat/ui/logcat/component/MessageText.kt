package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.filter.LogFilterMatcher
import com.melihaksoy.betterlogcat.format.JsonFoldRegion
import com.melihaksoy.betterlogcat.format.JsonFolding
import com.melihaksoy.betterlogcat.format.JsonHighlighter
import com.melihaksoy.betterlogcat.format.StackFrame
import com.melihaksoy.betterlogcat.format.StackFrameParser
import com.melihaksoy.betterlogcat.format.UrlFinder
import com.melihaksoy.betterlogcat.format.UrlMatch
import com.melihaksoy.betterlogcat.format.UrlShortener
import com.melihaksoy.betterlogcat.model.HighlightRule
import com.melihaksoy.betterlogcat.model.JsonContent
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.network.HttpMessage
import com.melihaksoy.betterlogcat.network.KtorLog
import com.melihaksoy.betterlogcat.network.NetworkCall
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
    val duration: (Long) -> String,
    /** A folded JSON block: its count of keys, or of items when the block is an array (the Boolean). */
    val folded: (Int, Boolean) -> String,
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
    /** Base URLs to hide. The network pane passes none: it shows full addresses. */
    urls: UrlShortener = UrlShortener.None,
    /** The HTTP call of this entry. A collapsed response shows how long the call took. */
    call: NetworkCall? = null,
    /** Folded JSON blocks, by their place among the blocks that can fold (see [JsonFolding]). */
    jsonFolds: Set<Int> = emptySet(),
    labels: MessageLabels,
    onOpenFrame: (StackFrame) -> Unit,
    onOpenUrl: (String) -> Unit,
    onShowAll: () -> Unit,
    onToggleJsonFold: (Int) -> Unit = {},
): AnnotatedString {
    // Where pretty JSON parts landed in the text: only those can fold.
    val prettyParts = ArrayList<IntRange>()
    val text = buildAnnotatedString {
        val append = { part: String ->
            appendMessage(
                text = part,
                textColor = textColor,
                colors = colors,
                highlighter = highlighter,
                keywords = keywords,
                urls = urls,
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
                urls = urls,
                onOpenUrl = onOpenUrl,
            )
        }
        val appendPrettyJsonPart = { json: String ->
            val start = length
            // A gutter, so even the outer block has room for its fold chevron.
            appendJsonPart(json.lineSequence().joinToString(separator = "\n") { JSON_GUTTER + it })
            prettyParts += start until length
        }
        val json = entry.json
        when {
            isExpanded && json is JsonContent.Found -> appendExpandedJson(
                message = entry.message,
                json = json,
                prettyJson = prettyJson,
                append = append,
                appendJsonPart = appendJsonPart,
                appendPrettyJsonPart = appendPrettyJsonPart,
            )
            isExpanded || !entry.isExpandable -> append(entry.message)
            else -> appendCollapsed(
                entry = entry,
                call = call,
                colors = colors,
                labels = labels,
                append = append,
                appendJsonPart = appendJsonPart,
                onOpenUrl = onOpenUrl,
            )
        }
        colorHttpStatus(
            entry = entry,
            colors = colors,
        )
    }
    val folded = if (prettyParts.isEmpty()) {
        text
    } else {
        foldJson(
            text = text,
            parts = prettyParts,
            folds = jsonFolds,
            colors = colors,
            labels = labels,
            onToggle = onToggleJsonFold,
        )
    }
    return if (isExpanded && !isFullyShown) {
        capped(
            text = folded,
            colors = colors,
            labels = labels,
            onShowAll = onShowAll,
        )
    } else {
        folded
    }
}

/**
 * Colors the status code of an HTTP response by its class, as in the network panel. Every form of the
 * message starts with the same first line, so the code sits where it sits in the message.
 */
private fun AnnotatedString.Builder.colorHttpStatus(
    entry: LogEntry,
    colors: LogColors,
) {
    val http = HttpMessage.of(message = entry.message) ?: return
    val code = http.statusCode ?: return
    if (http.statusStart < 0 || http.statusStart + STATUS_CHARS > length) return
    addStyle(
        style = SpanStyle(
            color = colors.httpStatus(statusCode = code),
            fontWeight = FontWeight.SemiBold,
        ),
        start = http.statusStart,
        end = http.statusStart + STATUS_CHARS,
    )
}

private const val STATUS_CHARS = 3

/**
 * Shows each folded JSON block as `▸ { … 12 keys }`. The line that opens a block (`▾ "data": {`) folds and
 * unfolds it on click; so does the folded text. A block inside a folded block keeps its own state.
 * Each shown block carries a [JsonPath.TAG] annotation from its key to its closer, so a split piece
 * still knows which blocks are around its lines.
 */
private fun foldJson(
    text: AnnotatedString,
    parts: List<IntRange>,
    folds: Set<Int>,
    colors: LogColors,
    labels: MessageLabels,
    onToggle: (Int) -> Unit,
): AnnotatedString {
    val regions = parts.flatMap { part ->
        JsonFolding.regions(
            text = text,
            start = part.first,
            end = part.last + 1,
        )
    }
    if (regions.isEmpty()) return text
    val toggleStyles = TextLinkStyles(hoveredStyle = SpanStyle(background = colors.link.copy(alpha = FOLD_HOVER_TINT)))
    return buildAnnotatedString {
        var cursor = 0
        // Unfolded blocks around the cursor, inner last. Each one has its path annotation pushed.
        val around = ArrayList<JsonFoldRegion>()
        val closeBefore = { position: Int ->
            while (around.isNotEmpty() && around.last().close < position) {
                val block = around.removeAt(around.lastIndex)
                append(text.subSequence(cursor, block.close + 1))
                cursor = block.close + 1
                pop()
            }
        }
        regions.forEachIndexed { ordinal, region ->
            if (region.open < cursor) return@forEachIndexed
            closeBefore(region.open)
            val lineStart = maxOf(text.lastIndexOf(char = '\n', startIndex = region.open) + 1, cursor)
            val keyStart = (lineStart..region.open).first { !text[it].isWhitespace() }
            val key = text.text.substring(keyStart, region.open).trim().removeSuffix(":").trim().removeSurrounding("\"")
            val parent = around.lastOrNull()
            val isFolded = ordinal in folds
            // The chevron takes the two spaces before the key; the gutter makes sure they are there.
            val markStart = if (keyStart - JSON_GUTTER.length >= lineStart) keyStart - JSON_GUTTER.length else keyStart
            append(text.subSequence(cursor, markStart))
            pushStringAnnotation(
                tag = JsonPath.TAG,
                annotation = JsonPath.encode(
                    item = JsonPathItem(
                        block = ordinal,
                        label = when {
                            key.isNotEmpty() -> key
                            parent?.isArray == true -> "[${region.index}]"
                            region.isArray -> "[ ]"
                            else -> "{ }"
                        },
                    ),
                ),
            )
            if (markStart < keyStart) {
                withStyle(style = SpanStyle(color = colors.muted)) {
                    append(if (isFolded) FOLDED_CHEVRON else UNFOLDED_CHEVRON)
                }
            }
            append(text.subSequence(keyStart, region.open + 1))
            val toggle = LinkAnnotation.Clickable(
                tag = "fold",
                styles = toggleStyles,
                linkInteractionListener = { onToggle(ordinal) },
            )
            addLink(
                clickable = toggle,
                start = length - (region.open + 1 - markStart),
                end = length,
            )
            if (isFolded) {
                withLink(link = toggle) {
                    withStyle(
                        style = SpanStyle(
                            color = colors.muted,
                            fontStyle = FontStyle.Italic,
                        ),
                    ) {
                        append(" … ${labels.folded(region.children, region.isArray)} ")
                    }
                }
                append(text.subSequence(region.close, region.close + 1))
                cursor = region.close + 1
                pop()
            } else {
                around += region
                cursor = region.open + 1
            }
        }
        closeBefore(Int.MAX_VALUE)
        append(text.subSequence(cursor, text.length))
    }
}

private const val FOLD_HOVER_TINT = 0.16f

/** Two characters, as wide as [JSON_GUTTER], so a chevron never moves the key. */
private const val UNFOLDED_CHEVRON = "▾ "
private const val FOLDED_CHEVRON = "▸ "
private const val JSON_GUTTER = "  "

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
    appendPrettyJsonPart: (String) -> Unit,
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
            appendPrettyJsonPart(block.pretty)
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

/** One compact line: [collapsedHead], a preview of the main JSON part, then what is hidden. */
private fun AnnotatedString.Builder.appendCollapsed(
    entry: LogEntry,
    call: NetworkCall?,
    colors: LogColors,
    labels: MessageLabels,
    append: (String) -> Unit,
    appendJsonPart: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val json = entry.json as? JsonContent.Found
    val head = collapsedHead(
        entry = entry,
        json = json,
        call = call,
        labels = labels,
    )
    append(head.firstLine)
    head.duration?.let { duration ->
        appendMuted(
            text = "  ·  ",
            colors = colors,
        )
        appendMuted(
            text = duration,
            colors = colors,
        )
    }
    head.address?.let { address ->
        appendMuted(
            text = "  ·  ",
            colors = colors,
        )
        append(address)
    }
    if (json != null) {
        append(" ")
        val preview = json.main.minified
        appendJsonPart(if (preview.length > JSON_PREVIEW_CHARS) preview.take(JSON_PREVIEW_CHARS) + "…" else preview)
    }
}

/** Characters in the one-line view [appendCollapsed] builds for [entry], for the sideways scroll width. */
internal fun collapsedLength(
    entry: LogEntry,
    call: NetworkCall?,
    labels: MessageLabels,
): Int {
    val json = entry.json as? JsonContent.Found
    val head = collapsedHead(
        entry = entry,
        json = json,
        call = call,
        labels = labels,
    )
    var length = head.firstLine.length
    head.duration?.let { length += URL_SEPARATOR_CHARS + it.length }
    head.address?.let { length += URL_SEPARATOR_CHARS + it.length }
    if (json != null) length += 1 + minOf(json.main.minified.length, JSON_PREVIEW_CHARS + 1)
    return length
}

/** The text of the one-line view before the JSON preview. [duration] and [address] each go after a separator. */
private class CollapsedHead(
    val firstLine: String,
    val duration: String?,
    val address: String?,
)

/**
 * The start of the first line, then the first web address when the first line has none (so a response
 * shows which request it answers). A Ktor request or response shows its method before the address, and
 * a Ktor response how long the call took. OkHttp writes both in its first line already.
 */
private fun collapsedHead(
    entry: LogEntry,
    json: JsonContent.Found?,
    call: NetworkCall?,
    labels: MessageLabels,
): CollapsedHead {
    val firstLine = entry.lines.first()
    val method = KtorLog.method(message = entry.message)
    val isRequest = method != null && firstLine.startsWith(KtorLog.REQUEST)
    var text = if (json != null && json.main.start < firstLine.length) firstLine.substring(0, json.main.start) else firstLine
    if (isRequest) text = "${KtorLog.REQUEST} $method ${text.substringAfter(KtorLog.REQUEST).trimStart()}"
    val url = if (firstLine.contains("http")) {
        null
    } else {
        UrlFinder.find(text = entry.message.take(URL_SEARCH_CHARS)).firstOrNull()?.let { entry.message.substring(it.start, it.end) }
    }
    val isResponse = method != null && !isRequest
    return CollapsedHead(
        firstLine = if (text.length > TEXT_PREVIEW_CHARS) text.take(TEXT_PREVIEW_CHARS) + "…" else text.trimEnd(),
        duration = call
            ?.takeIf { isResponse && it.responseEntryId == entry.id && it.durationMillis >= 0 }
            ?.let { labels.duration(it.durationMillis) },
        address = when {
            !isResponse -> url
            url == null -> method
            else -> "$method $url"
        },
    )
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
    urls: UrlShortener,
    onOpenUrl: (String) -> Unit,
) {
    val shown = urls.shorten(text = json)
    val start = length
    pushStyle(style = SpanStyle(color = textColor))
    append(shown.text)
    pop()
    keywords.apply(
        builder = this,
        text = shown.text,
        offset = start,
    )
    JsonHighlighter.tokens(json = shown.text).forEach { token ->
        addStyle(
            style = SpanStyle(color = colors.json(kind = token.kind)),
            start = start + token.start,
            end = start + token.end,
        )
    }
    addUrlLinks(
        links = shown.links,
        offset = start,
        colors = colors,
        onOpenUrl = onOpenUrl,
    )
    highlighter.ranges(text = shown.text).forEach { range ->
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
    links: List<UrlMatch>,
    offset: Int,
    colors: LogColors,
    onOpenUrl: (String) -> Unit,
) {
    links.forEach { match ->
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
    urls: UrlShortener,
    onOpenFrame: (StackFrame) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    // Compose draws a tab as one narrow space, so stack traces lose their indent.
    val shown = urls.shorten(text = text.replace("\t", TAB_SPACES))
    val display = shown.text
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
        links = shown.links,
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
