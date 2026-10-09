package com.melihaksoy.betterlogcat.ide

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings

class BookmarkStoreTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        BetterLogcatSettings.getInstance().loadState(BetterLogcatSettings.SettingsState())
    }

    fun `test bookmarks stay in log order and toggle off`() {
        val store = BookmarkStore()
        store.toggle(entry = entry(seconds = 20, message = "later"), processName = APP, deviceName = DEVICE)
        store.toggle(entry = entry(seconds = 10, message = "earlier"), processName = APP, deviceName = DEVICE)

        assertEquals(listOf("earlier", "later"), store.bookmarks.value.map { it.message })

        store.toggle(entry = entry(seconds = 10, message = "earlier"), processName = APP, deviceName = DEVICE)
        assertEquals(listOf("later"), store.bookmarks.value.map { it.message })
    }

    fun `test saved bookmarks load back through the cache file format`() {
        val saved = BookmarkStore()
        saved.toggle(entry = entry(seconds = 10, message = "FATAL EXCEPTION: main\n\tat Foo.bar(Foo.kt:3)"), processName = APP, deviceName = DEVICE)
        val element = XmlSerializer.serialize(saved.state)

        val loaded = BookmarkStore()
        loaded.loadState(state = XmlSerializer.deserialize(element, BookmarkStore.BookmarksState::class.java))

        assertEquals(saved.bookmarks.value, loaded.bookmarks.value)
    }

    fun `test characters that xml cannot hold are replaced`() {
        assertEquals("a�b😀\tc", BookmarkStore.xmlSafe(text = "a\u0001b😀\tc"))
        assertEquals("x�", BookmarkStore.xmlSafe(text = "x\uD83D"))
    }

    fun `test clear keeps bookmarks only when the setting is on`() {
        val store = BookmarkStore()
        store.toggle(entry = entry(seconds = 10, message = "kept"), processName = APP, deviceName = DEVICE)
        store.onLogCleared()
        assertEquals(1, store.bookmarks.value.size)

        BetterLogcatSettings.getInstance().state.keepBookmarks = false
        store.onLogCleared()
        assertTrue(store.bookmarks.value.isEmpty())
        assertTrue(store.state.items.isEmpty())
    }

    private fun entry(
        seconds: Long,
        message: String,
    ) = LogEntry(
        id = seconds,
        epochSeconds = 1_791_540_000L + seconds,
        nanos = 5_000,
        pid = 14_302,
        tid = 14_330,
        level = LogLevel.ERROR,
        tag = "AndroidRuntime",
        message = message,
    )

    private companion object {
        const val APP = "com.example.app"
        const val DEVICE = "Pixel 10 Pro XL"
    }
}
