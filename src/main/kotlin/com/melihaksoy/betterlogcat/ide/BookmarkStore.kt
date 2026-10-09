package com.melihaksoy.betterlogcat.ide

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.melihaksoy.betterlogcat.model.Bookmark
import com.melihaksoy.betterlogcat.model.BookmarkKey
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Bookmarked lines of one project, shared by every pane. With "keep bookmarks" on (the default) they
 * are saved in the IDE cache folder, not in the project, and survive Clear and a restart.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "BetterLogcatBookmarks",
    storages = [Storage(StoragePathMacros.CACHE_FILE)],
)
class BookmarkStore : PersistentStateComponent<BookmarkStore.BookmarksState> {

    class BookmarksState : BaseState() {
        var items by list<BookmarkState>()
    }

    class BookmarkState : BaseState() {
        var epochSeconds by property(defaultValue = 0L)
        var nanos by property(defaultValue = 0)
        var pid by property(defaultValue = 0)
        var tid by property(defaultValue = 0)
        var level by string(defaultValue = LogLevel.INFO.name)
        var tag by string(defaultValue = "")
        var message by string(defaultValue = "")
        var processName by string(defaultValue = "")
        var deviceName by string(defaultValue = "")
    }

    private val settings = BetterLogcatSettings.getInstance()
    private val _bookmarks = MutableStateFlow(value = emptyList<Bookmark>())

    /** Oldest first, in log order. */
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    fun toggle(
        entry: LogEntry,
        processName: String,
        deviceName: String,
    ) {
        val key = BookmarkKey.of(entry = entry)
        _bookmarks.update { current ->
            if (current.any { it.key == key }) {
                current.filter { it.key != key }
            } else {
                val added = Bookmark.of(
                    entry = entry,
                    processName = processName,
                    deviceName = deviceName,
                )
                (current + added).sortedWith(LOG_ORDER).takeLast(n = MAX_BOOKMARKS)
            }
        }
    }

    fun remove(key: BookmarkKey) {
        _bookmarks.update { current -> current.filter { it.key != key } }
    }

    /** The log was cleared. Bookmarks stay only when the user keeps them. */
    fun onLogCleared() {
        if (!settings.state.keepBookmarks) _bookmarks.value = emptyList()
    }

    override fun getState(): BookmarksState = BookmarksState().also { state ->
        if (settings.state.keepBookmarks) state.items = _bookmarks.value.map { it.toState() }.toMutableList()
    }

    override fun loadState(state: BookmarksState) {
        if (!settings.state.keepBookmarks) return
        _bookmarks.value = state.items.map { it.toBookmark() }.sortedWith(LOG_ORDER).takeLast(n = MAX_BOOKMARKS)
    }

    companion object {
        const val MAX_BOOKMARKS = 1_000

        /** Longer messages are cut before they are saved, so the cache file stays small. */
        const val MAX_SAVED_MESSAGE = 512 * 1_024

        private val LOG_ORDER = compareBy<Bookmark>({ it.key.epochSeconds }, { it.key.nanos })

        fun getInstance(project: Project): BookmarkStore = project.service()

        private fun Bookmark.toState(): BookmarkState = BookmarkState().also {
            it.epochSeconds = key.epochSeconds
            it.nanos = key.nanos
            it.pid = key.pid
            it.tid = key.tid
            it.level = level.name
            it.tag = xmlSafe(text = key.tag)
            it.message = xmlSafe(text = message.take(n = MAX_SAVED_MESSAGE))
            it.processName = xmlSafe(text = processName)
            it.deviceName = xmlSafe(text = deviceName)
        }

        private fun BookmarkState.toBookmark(): Bookmark = Bookmark(
            key = BookmarkKey(
                epochSeconds = epochSeconds,
                nanos = nanos,
                pid = pid,
                tid = tid,
                tag = tag.orEmpty(),
            ),
            level = LogLevel.entries.firstOrNull { it.name == level } ?: LogLevel.INFO,
            message = message.orEmpty(),
            processName = processName.orEmpty(),
            deviceName = deviceName.orEmpty(),
        )

        /** Logs can hold characters that XML cannot store (control characters, lone surrogates). They become U+FFFD. */
        internal fun xmlSafe(text: String): String {
            if (text.all { isXmlChar(char = it) && !it.isSurrogate() }) return text
            val builder = StringBuilder(text.length)
            var index = 0
            while (index < text.length) {
                val char = text[index]
                when {
                    char.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate() -> {
                        builder.append(char).append(text[index + 1])
                        index++
                    }
                    char.isSurrogate() || !isXmlChar(char = char) -> builder.append('�')
                    else -> builder.append(char)
                }
                index++
            }
            return builder.toString()
        }

        private fun isXmlChar(char: Char): Boolean =
            char == '\t' || char == '\n' || char == '\r' || char in ' '..'퟿' || char in '\uD800'..'\uDFFF' || char in ''..'�'
    }
}
