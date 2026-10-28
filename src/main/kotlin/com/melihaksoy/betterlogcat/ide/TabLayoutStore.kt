package com.melihaksoy.betterlogcat.ide

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

/** Open tabs and their split panes, per project, in the workspace file (not shared through version control). */
@Service(Service.Level.PROJECT)
@State(
    name = "BetterLogcatTabs",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class TabLayoutStore : SimplePersistentStateComponent<TabLayoutStore.TabsState>(TabsState()) {

    class TabsState : BaseState() {
        /** One encoded pane tree per tab, in tab order. */
        var tabs by list<String>()
        var selectedTab by property(defaultValue = 0)

        /** False until the first save, so a first run starts with one default tab. */
        var isSaved by property(defaultValue = false)

        /** The project app started last, so the first start after a restart can still count as a switch. */
        var lastLaunchedApp by string(defaultValue = null)
    }
}
