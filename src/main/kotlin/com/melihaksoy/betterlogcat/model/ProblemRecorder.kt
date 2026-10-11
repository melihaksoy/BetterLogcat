package com.melihaksoy.betterlogcat.model

/** One "Skipped N frames" line from Choreographer: the main thread was too busy to draw. */
data class FrameSkip(
    val epochMillis: Long,
    val frames: Int,
)

/**
 * Collects the problems of the current session as lines arrive. It keeps the ids of each problem's
 * lines that may still be in the log, for jumping to them. Not thread safe: use it from the buffer
 * dispatcher only.
 */
class ProblemRecorder {

    private val problems = LinkedHashMap<String, SavedProblem>()
    private val liveIds = HashMap<String, ArrayDeque<Long>>()
    private val frameSkips = ArrayDeque<FrameSkip>()

    /** True after a change, until the owner reads [snapshot]. */
    var isDirty: Boolean = false
        private set

    val size: Int
        get() = problems.size

    /** Adds [entry] when it is a problem whose key is not in [ignoredKeys]. Returns its kind, or null. */
    fun record(
        entry: LogEntry,
        processName: String,
        ignoredKeys: Set<String>,
    ): ProblemKind? {
        recordFrameSkip(entry = entry)
        val kind = entry.problem ?: return null
        val description = ProblemDescriptions.describe(
            entry = entry,
            kind = kind,
        )
        if (description.key in ignoredKeys) return null
        val millis = entry.epochSeconds * 1_000 + entry.nanos / 1_000_000
        val existing = problems[description.key]
        if (existing == null && !makeRoom(kind = kind)) return null
        problems[description.key] = SavedProblem(
            key = description.key,
            kind = kind,
            title = description.title,
            detail = description.detail,
            tag = entry.tag,
            count = (existing?.count ?: 0) + 1,
            firstEpochMillis = existing?.firstEpochMillis ?: millis,
            lastEpochMillis = millis,
            level = entry.level,
            pid = entry.pid,
            tid = entry.tid,
            processName = processName,
            sample = entry.message.take(n = MAX_SAMPLE),
        )
        val ids = liveIds.getOrPut(key = description.key) { ArrayDeque() }
        ids.addLast(entry.id)
        if (ids.size > MAX_LIVE_IDS) ids.removeFirst()
        isDirty = true
        return kind
    }

    /** Problems so far: worst kind first, then the most recent first. */
    fun snapshot(): List<SavedProblem> {
        isDirty = false
        return sorted(problems = problems.values)
    }

    /** Ids of [key]'s lines, oldest first. Lines trimmed from the log may be among them. */
    fun liveIds(key: String): List<Long> = liveIds[key]?.toList().orEmpty()

    fun frameSkips(): List<FrameSkip> = frameSkips.toList()

    fun remove(key: String) {
        problems.remove(key = key)
        liveIds.remove(key = key)
        isDirty = true
    }

    fun clear() {
        problems.clear()
        liveIds.clear()
        frameSkips.clear()
        isDirty = true
    }

    /** At the limit, a new problem replaces the oldest one of a kind no worse than its own. */
    private fun makeRoom(kind: ProblemKind): Boolean {
        if (problems.size < MAX_PROBLEMS) return true
        val victim = problems.values
            .filter { it.kind >= kind }
            .maxWithOrNull(compareBy<SavedProblem> { it.kind }.thenByDescending { it.lastEpochMillis })
            ?: return false
        remove(key = victim.key)
        return true
    }

    private fun recordFrameSkip(entry: LogEntry) {
        if (entry.tag != CHOREOGRAPHER) return
        val frames = SKIPPED.find(input = entry.message)?.groupValues?.get(1)?.toIntOrNull() ?: return
        frameSkips.addLast(
            FrameSkip(
                epochMillis = entry.epochSeconds * 1_000 + entry.nanos / 1_000_000,
                frames = frames,
            ),
        )
        if (frameSkips.size > MAX_FRAME_SKIPS) frameSkips.removeFirst()
        isDirty = true
    }

    companion object {
        const val MAX_PROBLEMS = 300
        const val MAX_LIVE_IDS = 500

        /** Saved samples are cut, so the workspace file stays small. A stack trace fits easily. */
        const val MAX_SAMPLE = 16 * 1_024
        private const val MAX_FRAME_SKIPS = 2_000
        private const val CHOREOGRAPHER = "Choreographer"
        private val SKIPPED = Regex(pattern = """Skipped (\d+) frames""")

        fun sorted(problems: Collection<SavedProblem>): List<SavedProblem> =
            problems.sortedWith(compareBy<SavedProblem> { it.kind }.thenByDescending { it.lastEpochMillis })
    }
}
