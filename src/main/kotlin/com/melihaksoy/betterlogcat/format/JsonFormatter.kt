package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.JsonBlock
import com.melihaksoy.betterlogcat.model.JsonContent

/**
 * Small strict JSON reader that validates and re-prints in one pass. No dependencies, so it is
 * safe to call on any message. Detection needs the JSON to run to the end of the message.
 */
object JsonFormatter {

    private const val MIN_LENGTH = 2
    private const val INDENT = "  "

    private const val MAX_FAILED_ATTEMPTS = 8
    private const val MAX_BLOCKS = 24
    /** Stricter start for parts that do not parse: a quote right after the brackets, so `[{id=7}]` (a Bundle) is not JSON. */
    private val lenientStart = Regex(pattern = """[{\[]\s*(?:[{\[]\s*)?"""")

    /**
     * Finds every JSON object or array (with at least one string) in a message. Text may sit around
     * and between them: `RESPONSE 200 ... -> nel: {...} ... BODY START {...} BODY END` gives two parts.
     * A last part with no closing bracket (cut off by the logger) still counts, as an incomplete part.
     */
    fun detect(message: String): JsonContent {
        val blocks = ArrayList<JsonBlock>()
        var index = 0
        var failed = 0
        while (index < message.length && failed < MAX_FAILED_ATTEMPTS && blocks.size < MAX_BLOCKS) {
            val start = nextOpening(
                message = message,
                from = index,
            )
            if (start < 0) break
            val end = matchingClose(
                text = message,
                start = start,
            )
            if (end < 0) {
                if (lenientStart.matchesAt(input = message, index = start)) {
                    blocks += lenientBlock(
                        message = message,
                        start = start,
                        end = message.length,
                    )
                }
                break
            }
            // Brackets that balance but do not parse (a piece lost between chunks, a broken string)
            // still read as JSON when they open like JSON.
            val block = completeBlock(
                message = message,
                start = start,
                end = end + 1,
            ) ?: lenientBlock(
                message = message,
                start = start,
                end = end + 1,
            ).takeIf { lenientStart.matchesAt(input = message, index = start) }
            if (block == null) {
                failed++
                index = start + 1
            } else {
                blocks += block
                index = end + 1
            }
        }
        if (blocks.isEmpty()) return JsonContent.None
        // The body: a part on its own line beats an inline header value; size breaks ties.
        val main = blocks.maxWith(
            compareBy<JsonBlock> { startsLine(message = message, index = it.start) }.thenBy { it.end - it.start },
        )
        return JsonContent.Found(
            blocks = blocks.map { it.copy(isInline = it !== main && !startsLine(message = message, index = it.start)) },
            main = main,
            prefix = message.substring(0, main.start),
            suffix = message.substring(main.end),
        )
    }

    private fun completeBlock(
        message: String,
        start: Int,
        end: Int,
    ): JsonBlock? {
        val candidate = message.substring(start, end)
        if (candidate.length <= MIN_LENGTH || !candidate.contains('"')) return null
        val pretty = format(
            json = candidate,
            pretty = true,
        ) ?: return null
        val minified = format(
            json = candidate,
            pretty = false,
        ) ?: return null
        return JsonBlock(
            start = start,
            end = end,
            raw = candidate,
            pretty = pretty,
            minified = minified,
            isComplete = true,
            isInline = false,
        )
    }

    /** A part that does not parse: indented by [lenientPretty], copied as it is. */
    private fun lenientBlock(
        message: String,
        start: Int,
        end: Int,
    ): JsonBlock {
        val raw = message.substring(start, end)
        return JsonBlock(
            start = start,
            end = end,
            raw = raw,
            pretty = lenientPretty(json = raw),
            minified = raw,
            isComplete = false,
            isInline = false,
        )
    }

    /**
     * Indents JSON that does not parse, so a body cut off by the logger still reads like JSON. Walks
     * brackets, commas and colons outside strings and never fails. Text inside strings stays as it is.
     */
    fun lenientPretty(json: String): String {
        val out = StringBuilder(json.length + json.length / 4)
        var depth = 0
        var isInString = false
        var index = 0
        while (index < json.length) {
            val char = json[index]
            if (isInString) {
                out.append(char)
                if (char == '\\' && index + 1 < json.length) {
                    out.append(json[index + 1])
                    index++
                } else if (char == '"') {
                    isInString = false
                }
            } else {
                when (char) {
                    '"' -> {
                        out.append(char)
                        isInString = true
                    }
                    '{', '[' -> {
                        var next = index + 1
                        while (next < json.length && json[next].isWhitespace()) next++
                        if (next < json.length && (json[next] == '}' || json[next] == ']')) {
                            out.append(char).append(json[next])
                            index = next
                        } else {
                            depth++
                            out.append(char)
                            lenientNewLine(out = out, depth = depth)
                        }
                    }
                    '}', ']' -> {
                        depth = (depth - 1).coerceAtLeast(minimumValue = 0)
                        lenientNewLine(out = out, depth = depth)
                        out.append(char)
                    }
                    ',' -> {
                        out.append(char)
                        lenientNewLine(out = out, depth = depth)
                    }
                    ':' -> out.append(": ")
                    ' ', '\t', '\n', '\r' -> Unit
                    else -> out.append(char)
                }
            }
            index++
        }
        return out.toString()
    }

    private fun lenientNewLine(
        out: StringBuilder,
        depth: Int,
    ) {
        out.append('\n')
        repeat(times = depth) { out.append(INDENT) }
    }

    /** True when only spaces sit between the previous line break (or the start) and [index]. */
    private fun startsLine(
        message: String,
        index: Int,
    ): Boolean {
        var position = index - 1
        while (position >= 0 && (message[position] == ' ' || message[position] == '\t')) position--
        return position < 0 || message[position] == '\n'
    }

    /** Index of the bracket that closes the one at [start], skipping strings. -1 when unbalanced. */
    private fun matchingClose(
        text: String,
        start: Int,
    ): Int {
        var depth = 0
        var index = start
        var isInString = false
        while (index < text.length) {
            val char = text[index]
            if (isInString) {
                when (char) {
                    '\\' -> index++
                    '"' -> isInString = false
                }
            } else {
                when (char) {
                    '"' -> isInString = true
                    '{', '[' -> depth++
                    '}', ']' -> {
                        depth--
                        if (depth == 0) return index
                    }
                }
            }
            index++
        }
        return -1
    }

    private fun nextOpening(
        message: String,
        from: Int,
    ): Int {
        for (index in from until message.length) {
            val char = message[index]
            if (char == '{' || char == '[') return index
        }
        return -1
    }

    /** Returns the re-printed JSON, or null when [json] is not one valid JSON value. */
    fun format(
        json: String,
        pretty: Boolean,
    ): String? {
        val writer = Writer(
            input = json,
            pretty = pretty,
        )
        return try {
            writer.skipWhitespace()
            writer.value(depth = 0)
            writer.skipWhitespace()
            if (writer.position != json.length) null else writer.output.toString()
        } catch (_: MalformedJsonException) {
            null
        }
    }

    private class MalformedJsonException : RuntimeException() {
        override fun fillInStackTrace(): Throwable = this
    }

    private class Writer(
        private val input: String,
        private val pretty: Boolean,
    ) {
        val output = StringBuilder(input.length + input.length / 4)
        var position = 0

        fun value(depth: Int) {
            if (depth > 256) fail()
            when (peek()) {
                '{' -> container(
                    depth = depth,
                    open = '{',
                    close = '}',
                    isObject = true,
                )
                '[' -> container(
                    depth = depth,
                    open = '[',
                    close = ']',
                    isObject = false,
                )
                '"' -> string()
                't' -> literal(word = "true")
                'f' -> literal(word = "false")
                'n' -> literal(word = "null")
                else -> number()
            }
        }

        private fun container(
            depth: Int,
            open: Char,
            close: Char,
            isObject: Boolean,
        ) {
            expect(char = open)
            output.append(open)
            skipWhitespace()
            if (peek() == close) {
                position++
                output.append(close)
                return
            }
            var first = true
            while (true) {
                if (!first) output.append(',')
                first = false
                newLine(depth = depth + 1)
                if (isObject) {
                    if (peek() != '"') fail()
                    string()
                    skipWhitespace()
                    expect(char = ':')
                    output.append(if (pretty) ": " else ":")
                    skipWhitespace()
                }
                value(depth = depth + 1)
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        position++
                        skipWhitespace()
                    }
                    close -> {
                        position++
                        newLine(depth = depth)
                        output.append(close)
                        return
                    }
                    else -> fail()
                }
            }
        }

        private fun string() {
            val start = position
            expect(char = '"')
            while (true) {
                if (position >= input.length) fail()
                when (input[position]) {
                    '"' -> {
                        position++
                        output.append(input, start, position)
                        return
                    }
                    '\\' -> position += 2
                    '\n' -> fail()
                    else -> position++
                }
            }
        }

        private fun literal(word: String) {
            if (!input.startsWith(word, position)) fail()
            position += word.length
            output.append(word)
        }

        private fun number() {
            val start = position
            if (peek() == '-') position++
            var digits = 0
            while (position < input.length && (input[position].isDigit() || input[position] in "+-.eE")) {
                if (input[position].isDigit()) digits++
                position++
            }
            if (digits == 0) fail()
            output.append(input, start, position)
        }

        private fun newLine(depth: Int) {
            if (!pretty) return
            output.append('\n')
            repeat(times = depth) { output.append(INDENT) }
        }

        fun skipWhitespace() {
            while (position < input.length && input[position].isWhitespace()) position++
        }

        private fun peek(): Char = if (position < input.length) input[position] else fail()

        private fun expect(char: Char) {
            if (peek() != char) fail()
            position++
        }

        private fun fail(): Nothing = throw MalformedJsonException()
    }
}
