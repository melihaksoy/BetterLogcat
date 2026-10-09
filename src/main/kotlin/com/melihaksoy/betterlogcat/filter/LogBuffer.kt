package com.melihaksoy.betterlogcat.filter

import com.melihaksoy.betterlogcat.model.LogEntry

/**
 * Holds all received entries and the filtered subset. Both lists stay in id order.
 * Not thread safe: use it from one coroutine dispatcher only.
 */
class LogBuffer(capacity: Int) {

    var capacity: Int = capacity.coerceAtLeast(minimumValue = MIN_CAPACITY)
        set(value) {
            field = value.coerceAtLeast(minimumValue = MIN_CAPACITY)
            trim()
        }

    private val all = ArrayList<LogEntry>()
    private val visible = ArrayList<LogEntry>()
    private var matcher: (LogEntry) -> Boolean = { true }
    private val tagCounts = HashMap<String, Int>()
    private val tagPids = HashMap<String, MutableSet<Int>>()

    val totalCount: Int
        get() = all.size

    val lastEntry: LogEntry?
        get() = all.lastOrNull()

    /** Entry count per tag over all received entries, filtered or not. */
    fun tagCountsSnapshot(): Map<String, Int> = HashMap(tagCounts)

    /** Process ids seen per tag, for telling app tags from system tags. A few pids per tag at most. */
    fun tagPidsSnapshot(): Map<String, Set<Int>> = tagPids.mapValues { it.value.toSet() }

    fun append(entries: List<LogEntry>) {
        if (entries.isEmpty()) return
        for (entry in entries) {
            if (mergeIntoPrevious(entry = entry)) continue
            all.add(entry)
            tagCounts.merge(entry.tag, 1, Int::plus)
            val pids = tagPids.getOrPut(entry.tag) { HashSet() }
            if (pids.size < MAX_PIDS_PER_TAG) pids.add(entry.pid)
            if (matcher(entry)) visible.add(entry)
        }
        trim()
    }

    /**
     * Loggers such as Ktor's cut long messages (big JSON responses) into pieces of up to ~4000 chars.
     * A piece that follows a long piece from the same thread, tag and level, within a moment, joins it.
     */
    private fun mergeIntoPrevious(entry: LogEntry): Boolean {
        val lookBack = (all.size - MERGE_LOOK_BACK).coerceAtLeast(minimumValue = 0)
        for (index in all.indices.reversed()) {
            if (index < lookBack) return false
            val previous = all[index]
            if (previous.tid != entry.tid) continue
            if (!isContinuation(previous = previous, next = entry)) return false
            val separator = if (previous.message.length >= CHUNK_LIMIT) "" else "\n"
            val merged = previous.withMessage(message = previous.message + separator + entry.message)
            all[index] = merged
            val visibleIndex = firstIndexAtLeast(id = merged.id)
            val wasVisible = visibleIndex < visible.size && visible[visibleIndex].id == merged.id
            val isVisible = matcher(merged)
            when {
                wasVisible && isVisible -> visible[visibleIndex] = merged
                wasVisible -> visible.removeAt(visibleIndex)
                isVisible -> visible.add(visibleIndex, merged)
            }
            return true
        }
        return false
    }

    private fun isContinuation(
        previous: LogEntry,
        next: LogEntry,
    ): Boolean {
        if (previous.pid != next.pid || previous.tag != next.tag || previous.level != next.level) return false
        if (previous.message.length < CHUNK_MIN) return false
        val gapNanos = (next.epochSeconds - previous.epochSeconds) * 1_000_000_000L + (next.nanos - previous.nanos)
        return gapNanos in 0..MERGE_WINDOW_NANOS
    }

    fun refilter(predicate: (LogEntry) -> Boolean) {
        matcher = predicate
        visible.clear()
        all.filterTo(destination = visible, predicate = predicate)
    }

    /** Replaces every entry, then filters again. Use for changes that rewrite messages, like retrace. */
    fun replaceAll(transform: (LogEntry) -> LogEntry) {
        for (index in all.indices) all[index] = transform(all[index])
        refilter(predicate = matcher)
    }

    /** Counts entries that match [predicate], stopping at [limit]. */
    fun count(
        predicate: (LogEntry) -> Boolean,
        limit: Int,
    ): Int {
        var count = 0
        for (index in all.indices.reversed()) {
            if (predicate(all[index]) && ++count >= limit) break
        }
        return count
    }

    fun clear() {
        all.clear()
        visible.clear()
        tagCounts.clear()
        tagPids.clear()
    }

    fun visibleSnapshot(): List<LogEntry> = ArrayList(visible)

    private fun trim() {
        if (all.size <= capacity) return
        // Drop an extra 10 % so trimming does not run on every batch.
        val dropCount = (all.size - capacity + capacity / 10).coerceAtMost(maximumValue = all.size)
        val firstKeptId = if (dropCount < all.size) all[dropCount].id else Long.MAX_VALUE
        val dropped = all.subList(0, dropCount)
        dropped.forEach { entry ->
            tagCounts.computeIfPresent(entry.tag) { _, count -> if (count <= 1) null else count - 1 }
        }
        dropped.clear()
        val visibleDrop = firstIndexAtLeast(id = firstKeptId)
        visible.subList(0, visibleDrop).clear()
    }

    private fun firstIndexAtLeast(id: Long): Int {
        var low = 0
        var high = visible.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (visible[middle].id < id) low = middle + 1 else high = middle
        }
        return low
    }

    private companion object {
        const val MIN_CAPACITY = 1_000
        const val MAX_PIDS_PER_TAG = 16
        const val MERGE_LOOK_BACK = 8

        /** Chunking loggers split between 3000 and 4000 chars, at a line break when they can. */
        const val CHUNK_MIN = 3_000
        const val CHUNK_LIMIT = 4_000
        const val MERGE_WINDOW_NANOS = 200_000_000L
    }
}
