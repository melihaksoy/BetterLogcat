package com.melihaksoy.betterlogcat.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProblemHistoryTest {

    private fun problem(
        key: String,
        lastEpochMillis: Long = 0,
    ) = SavedProblem(
        key = key,
        kind = ProblemKind.WARNING,
        title = key,
        detail = "",
        tag = "Tag",
        count = 1,
        firstEpochMillis = lastEpochMillis,
        lastEpochMillis = lastEpochMillis,
        level = LogLevel.WARN,
        pid = 1,
        tid = 1,
        processName = "",
        sample = key,
    )

    private fun session(
        id: String,
        started: Long,
        serial: String = "",
        problems: List<SavedProblem> = listOf(problem(key = "a")),
    ) = ProblemSession(
        id = id,
        title = id,
        deviceSerial = serial,
        startedEpochMillis = started,
        problems = problems,
    )

    @Test
    fun `saving keeps newest first, replaces by id and drops empty sessions`() {
        val saved = ProblemHistory.saved(sessions = listOf(session(id = "old", started = 1)), session = session(id = "new", started = 2))
        val replaced = ProblemHistory.saved(sessions = saved, session = session(id = "old", started = 1, problems = emptyList()))

        assertEquals(listOf("new", "old"), saved.map { it.id })
        assertEquals(listOf("new"), replaced.map { it.id })
    }

    @Test
    fun `deleting by age keeps the live session`() {
        val sessions = listOf(session(id = "live", started = 1), session(id = "recent", started = 50), session(id = "old", started = 2))

        assertEquals(listOf("live", "recent"), ProblemHistory.olderDeleted(sessions = sessions, cutoffEpochMillis = 10, keepId = "live").map { it.id })
    }

    @Test
    fun `deleting the last problem drops the session, rename skips blanks`() {
        val sessions = listOf(session(id = "s", started = 1))

        assertEquals(emptyList<ProblemSession>(), ProblemHistory.problemDeleted(sessions = sessions, id = "s", key = "a"))
        assertEquals("Run 2", ProblemHistory.renamed(sessions = sessions, id = "s", title = " Run 2 ").single().title)
        assertEquals("s", ProblemHistory.renamed(sessions = sessions, id = "s", title = "  ").single().title)
    }

    @Test
    fun `saved until is the newest problem of the device`() {
        val sessions = listOf(
            session(id = "a", started = 1, serial = "pixel", problems = listOf(problem(key = "x", lastEpochMillis = 70))),
            session(id = "b", started = 2, serial = "emulator", problems = listOf(problem(key = "y", lastEpochMillis = 90))),
        )

        assertEquals(70L, ProblemHistory.savedUntil(sessions = sessions, deviceSerial = "pixel"))
        assertEquals(Long.MIN_VALUE, ProblemHistory.savedUntil(sessions = sessions, deviceSerial = "other"))
    }
}
