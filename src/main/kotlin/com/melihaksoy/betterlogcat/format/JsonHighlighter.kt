package com.melihaksoy.betterlogcat.format

enum class JsonTokenKind {
    KEY,
    STRING,
    NUMBER,
    KEYWORD,
}

data class JsonToken(
    val start: Int,
    val end: Int,
    val kind: JsonTokenKind,
)

/** Finds token ranges in JSON printed by [JsonFormatter], for syntax colors. */
object JsonHighlighter {

    fun tokens(json: String): List<JsonToken> {
        val tokens = ArrayList<JsonToken>()
        var index = 0
        while (index < json.length) {
            val char = json[index]
            when {
                char == '"' -> {
                    val end = stringEnd(
                        json = json,
                        start = index,
                    )
                    var next = end
                    while (next < json.length && json[next] == ' ') next++
                    val kind = if (next < json.length && json[next] == ':') JsonTokenKind.KEY else JsonTokenKind.STRING
                    tokens += JsonToken(
                        start = index,
                        end = end,
                        kind = kind,
                    )
                    index = end
                }
                char == '-' || char.isDigit() -> {
                    val start = index
                    while (index < json.length && (json[index].isDigit() || json[index] in "+-.eE")) index++
                    tokens += JsonToken(
                        start = start,
                        end = index,
                        kind = JsonTokenKind.NUMBER,
                    )
                }
                char == 't' || char == 'f' || char == 'n' -> {
                    val start = index
                    while (index < json.length && json[index].isLetter()) index++
                    tokens += JsonToken(
                        start = start,
                        end = index,
                        kind = JsonTokenKind.KEYWORD,
                    )
                }
                else -> index++
            }
        }
        return tokens
    }

    private fun stringEnd(
        json: String,
        start: Int,
    ): Int {
        var index = start + 1
        while (index < json.length) {
            when (json[index]) {
                '\\' -> index += 2
                '"' -> return index + 1
                else -> index++
            }
        }
        return json.length
    }
}
