package com.melihaksoy.betterlogcat.toolwindow

import java.util.Base64

/** The saved shape of one tab: panes in nested splits, each with its device and filter. */
sealed interface PaneNode {

    data class Leaf(
        val serial: String,
        val query: String,
    ) : PaneNode

    data class Split(
        val isVertical: Boolean,
        val proportion: Float,
        val first: PaneNode,
        val second: PaneNode,
    ) : PaneNode

    /** One line of text. Parts are Base64 so any filter text survives. */
    fun encode(): String = when (this) {
        is Leaf -> "L:${b64(serial)}:${b64(query)}"
        is Split -> "S:${if (isVertical) "v" else "h"}:$proportion:${b64(first.encode())}:${b64(second.encode())}"
    }

    companion object {
        /** Null for broken or unknown input; the caller then starts a plain tab. */
        fun decode(text: String): PaneNode? = try {
            val parts = text.split(':')
            when (parts.firstOrNull()) {
                "L" -> Leaf(
                    serial = unb64(parts[1]),
                    query = unb64(parts[2]),
                )
                "S" -> Split(
                    isVertical = parts[1] == "v",
                    proportion = parts[2].toFloat().coerceIn(minimumValue = 0.1f, maximumValue = 0.9f),
                    first = decode(text = unb64(parts[3])) ?: return null,
                    second = decode(text = unb64(parts[4])) ?: return null,
                )
                else -> null
            }
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IndexOutOfBoundsException) {
            null
        }

        private fun b64(text: String): String = Base64.getEncoder().encodeToString(text.toByteArray())

        private fun unb64(text: String): String = String(Base64.getDecoder().decode(text))
    }
}
