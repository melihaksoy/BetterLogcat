package com.melihaksoy.betterlogcat.format

/** One `at com.example.Foo.bar(Foo.kt:42)` frame. [start] and [end] cover the `Foo.kt:42` link text. */
data class StackFrame(
    val start: Int,
    val end: Int,
    val className: String,
    val fileName: String,
    val line: Int,
)

object StackFrameParser {

    private val frameRegex = Regex(
        pattern = """\bat\s+((?:[\w$]+\.)+)[\w$<>-]+\(([\w$.-]+\.(?:java|kt|kts|scala|groovy)):(\d+)\)""",
    )

    fun containsFrame(text: String): Boolean = text.contains("at ") && frameRegex.containsMatchIn(input = text)

    fun findFrames(text: String): List<StackFrame> {
        if (!text.contains("at ")) return emptyList()
        return frameRegex.findAll(input = text).mapNotNull { match ->
            val classPart = match.groupValues[1].removeSuffix(".")
            val fileGroup = match.groups[2] ?: return@mapNotNull null
            val lineGroup = match.groups[3] ?: return@mapNotNull null
            StackFrame(
                start = fileGroup.range.first,
                end = lineGroup.range.last + 1,
                className = classPart,
                fileName = fileGroup.value,
                line = lineGroup.value.toIntOrNull() ?: return@mapNotNull null,
            )
        }.toList()
    }
}
