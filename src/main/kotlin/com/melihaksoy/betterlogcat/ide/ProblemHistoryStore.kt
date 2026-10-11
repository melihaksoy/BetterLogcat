package com.melihaksoy.betterlogcat.ide

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.ProblemHistory
import com.melihaksoy.betterlogcat.model.ProblemKind
import com.melihaksoy.betterlogcat.model.ProblemSession
import com.melihaksoy.betterlogcat.model.SavedProblem
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.TimeUnit

/**
 * Problem sessions of one project, shared by every pane, newest first. Saved in the project's
 * workspace file (`.idea/workspace.xml`, personal, not shared), so they survive Clear, a restart and
 * Invalidate Caches. Sessions older than the retention setting are dropped when the project opens.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "BetterLogcatProblemHistory",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class ProblemHistoryStore : PersistentStateComponent<ProblemHistoryStore.HistoryState> {

    class HistoryState : BaseState() {
        var sessions by list<SessionState>()
        var ignoredKeys by list<String>()
    }

    class SessionState : BaseState() {
        var id by string(defaultValue = "")
        var title by string(defaultValue = "")
        var deviceSerial by string(defaultValue = "")
        var startedEpochMillis by property(defaultValue = 0L)
        var problems by list<ProblemState>()
    }

    class ProblemState : BaseState() {
        var key by string(defaultValue = "")
        var kind by string(defaultValue = ProblemKind.WARNING.name)
        var title by string(defaultValue = "")
        var detail by string(defaultValue = "")
        var tag by string(defaultValue = "")
        var count by property(defaultValue = 0)
        var firstEpochMillis by property(defaultValue = 0L)
        var lastEpochMillis by property(defaultValue = 0L)
        var level by string(defaultValue = LogLevel.WARN.name)
        var pid by property(defaultValue = 0)
        var tid by property(defaultValue = 0)
        var processName by string(defaultValue = "")
        var sample by string(defaultValue = "")
    }

    private val settings = BetterLogcatSettings.getInstance()
    private val _sessions = MutableStateFlow(value = emptyList<ProblemSession>())
    private val _ignoredKeys = MutableStateFlow(value = emptySet<String>())

    /** Newest first. */
    val sessions: StateFlow<List<ProblemSession>> = _sessions.asStateFlow()

    /** Problems the user chose to ignore: they are not recorded or shown. */
    val ignoredKeys: StateFlow<Set<String>> = _ignoredKeys.asStateFlow()

    fun save(session: ProblemSession) = _sessions.update {
        ProblemHistory.saved(
            sessions = it,
            session = session,
        )
    }

    fun rename(
        id: String,
        title: String,
    ) = _sessions.update {
        ProblemHistory.renamed(
            sessions = it,
            id = id,
            title = title,
        )
    }

    fun delete(id: String) = _sessions.update {
        ProblemHistory.deleted(
            sessions = it,
            id = id,
        )
    }

    fun deleteOlderThan(
        cutoffEpochMillis: Long,
        keepId: String,
    ) = _sessions.update {
        ProblemHistory.olderDeleted(
            sessions = it,
            cutoffEpochMillis = cutoffEpochMillis,
            keepId = keepId,
        )
    }

    fun deleteProblem(
        id: String,
        key: String,
    ) = _sessions.update {
        ProblemHistory.problemDeleted(
            sessions = it,
            id = id,
            key = key,
        )
    }

    fun ignore(key: String) = _ignoredKeys.update { it + key }

    fun clearIgnored() {
        _ignoredKeys.value = emptySet()
    }

    override fun getState(): HistoryState = HistoryState().also { state ->
        state.sessions = _sessions.value.map { it.toState() }.toMutableList()
        state.ignoredKeys = _ignoredKeys.value.map { BookmarkStore.xmlSafe(text = it) }.toMutableList()
    }

    override fun loadState(state: HistoryState) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(settings.state.problemHistoryDays.toLong())
        _sessions.value = ProblemHistory.olderDeleted(
            sessions = state.sessions.map { it.toSession() }.sortedByDescending { it.startedEpochMillis },
            cutoffEpochMillis = cutoff,
        )
        _ignoredKeys.value = state.ignoredKeys.toSet()
    }

    companion object {
        fun getInstance(project: Project): ProblemHistoryStore = project.service()

        private fun ProblemSession.toState(): SessionState = SessionState().also {
            it.id = id
            it.title = BookmarkStore.xmlSafe(text = title)
            it.deviceSerial = BookmarkStore.xmlSafe(text = deviceSerial)
            it.startedEpochMillis = startedEpochMillis
            it.problems = problems.map { problem -> problem.toState() }.toMutableList()
        }

        private fun SavedProblem.toState(): ProblemState = ProblemState().also {
            it.key = BookmarkStore.xmlSafe(text = key)
            it.kind = kind.name
            it.title = BookmarkStore.xmlSafe(text = title)
            it.detail = BookmarkStore.xmlSafe(text = detail)
            it.tag = BookmarkStore.xmlSafe(text = tag)
            it.count = count
            it.firstEpochMillis = firstEpochMillis
            it.lastEpochMillis = lastEpochMillis
            it.level = level.name
            it.pid = pid
            it.tid = tid
            it.processName = BookmarkStore.xmlSafe(text = processName)
            it.sample = BookmarkStore.xmlSafe(text = sample)
        }

        private fun SessionState.toSession(): ProblemSession = ProblemSession(
            id = id.orEmpty(),
            title = title.orEmpty(),
            deviceSerial = deviceSerial.orEmpty(),
            startedEpochMillis = startedEpochMillis,
            problems = problems.map { it.toProblem() },
        )

        private fun ProblemState.toProblem(): SavedProblem = SavedProblem(
            key = key.orEmpty(),
            kind = ProblemKind.entries.firstOrNull { it.name == kind } ?: ProblemKind.WARNING,
            title = title.orEmpty(),
            detail = detail.orEmpty(),
            tag = tag.orEmpty(),
            count = count,
            firstEpochMillis = firstEpochMillis,
            lastEpochMillis = lastEpochMillis,
            level = LogLevel.entries.firstOrNull { it.name == level } ?: LogLevel.WARN,
            pid = pid,
            tid = tid,
            processName = processName.orEmpty(),
            sample = sample.orEmpty(),
        )
    }
}
