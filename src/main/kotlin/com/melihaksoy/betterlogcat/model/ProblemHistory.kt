package com.melihaksoy.betterlogcat.model

/** Changes to the saved problem sessions. Each returns a new list, newest session first. */
object ProblemHistory {

    const val MAX_SESSIONS = 50

    /** Adds or replaces [session]. A session with no problems is not kept. */
    fun saved(
        sessions: List<ProblemSession>,
        session: ProblemSession,
    ): List<ProblemSession> {
        val others = sessions.filter { it.id != session.id }
        val all = if (session.problems.isEmpty()) others else others + session
        return all.sortedByDescending { it.startedEpochMillis }.take(n = MAX_SESSIONS)
    }

    /** The newest problem time saved for [deviceSerial]: lines up to it are already in a session. */
    fun savedUntil(
        sessions: List<ProblemSession>,
        deviceSerial: String,
    ): Long = sessions
        .filter { it.deviceSerial == deviceSerial }
        .flatMap { it.problems }
        .maxOfOrNull { it.lastEpochMillis } ?: Long.MIN_VALUE

    /** A blank title changes nothing. */
    fun renamed(
        sessions: List<ProblemSession>,
        id: String,
        title: String,
    ): List<ProblemSession> {
        val name = title.trim()
        if (name.isEmpty()) return sessions
        return sessions.map { if (it.id == id) it.copy(title = name) else it }
    }

    fun deleted(
        sessions: List<ProblemSession>,
        id: String,
    ): List<ProblemSession> = sessions.filter { it.id != id }

    /** Deletes sessions that started before [cutoffEpochMillis], except [keepId] (the live one). */
    fun olderDeleted(
        sessions: List<ProblemSession>,
        cutoffEpochMillis: Long,
        keepId: String = "",
    ): List<ProblemSession> = sessions.filter { it.startedEpochMillis >= cutoffEpochMillis || it.id == keepId }

    /** Deletes one problem. A session left with none goes away. */
    fun problemDeleted(
        sessions: List<ProblemSession>,
        id: String,
        key: String,
    ): List<ProblemSession> = sessions
        .map { session -> if (session.id == id) session.copy(problems = session.problems.filter { it.key != key }) else session }
        .filter { it.problems.isNotEmpty() }
}
