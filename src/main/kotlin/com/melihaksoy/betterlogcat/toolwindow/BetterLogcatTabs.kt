package com.melihaksoy.betterlogcat.toolwindow

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.ide.BetterLogcatProjectService
import kotlinx.coroutines.delay
import com.melihaksoy.betterlogcat.ide.TabLayoutStore
import com.melihaksoy.betterlogcat.filter.QueryTerm
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import com.melihaksoy.betterlogcat.ui.logcat.tabTitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tabs of the Better Logcat tool window. Each tab holds a [LogcatPaneTree]; each pane keeps its own
 * device, package and filters. Adb and the device poll are shared through [BetterLogcatProjectService].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BetterLogcatTabs(
    private val project: Project,
    private val toolWindow: ToolWindow,
) {
    private val service = project.service<BetterLogcatProjectService>()
    private val trees = HashMap<Content, LogcatPaneTree>()

    init {
        toolWindow.contentManager.addContentManagerListener(
            object : ContentManagerListener {
                override fun contentRemoved(event: ContentManagerEvent) {
                    trees.remove(event.content)
                    saveTabs()
                }

                override fun selectionChanged(event: ContentManagerEvent) = saveTabs()
            },
        )
    }

    private val layoutStore = project.service<TabLayoutStore>()

    /** Reopens the tabs of the last session, or one new tab on a first run. */
    fun restoreTabs() {
        val saved = layoutStore.state
        val layouts = saved.tabs.mapNotNull { PaneNode.decode(text = it) }
        if (!saved.isSaved) {
            addTab()
        } else {
            layouts.forEach { addTab(layout = it) }
            toolWindow.contentManager.contents.getOrNull(index = saved.selectedTab)?.let { toolWindow.contentManager.setSelectedContent(it) }
        }
        // Device and filter changes have no single event; a light periodic save catches them.
        service.scope.launch {
            while (true) {
                delay(timeMillis = SAVE_INTERVAL_MILLIS)
                withContext(context = Dispatchers.EDT) { saveTabs() }
            }
        }
    }

    /** Adds a tab. Without a [layout] it starts on the device of the selected tab, with the last filter. */
    fun addTab(layout: PaneNode? = null) {
        val selected = toolWindow.contentManager.selectedContent?.let { trees[it] }
        val initialSerial = selected?.primary?.value?.viewModel?.state?.value?.selectedSerial.orEmpty()

        val tree = LogcatPaneTree(
            project = project,
            service = service,
            layout = layout ?: PaneNode.Leaf(
                serial = initialSerial,
                // New tabs start from the last filter; the very first one from package:mine.
                query = BetterLogcatSettings.getInstance().state.lastFilter ?: QueryTerm.Mine.text,
            ),
            onChanged = ::saveTabs,
        )
        val content = ContentFactory.getInstance().createContent(tree.component, "", false)
        content.isCloseable = true
        Disposer.register(content, tree)
        trees[content] = tree

        val job = SupervisorJob(parent = service.scope.coroutineContext.job)
        Disposer.register(content) { job.cancel() }
        CoroutineScope(context = service.scope.coroutineContext + job).launch {
            tree.primary
                .flatMapLatest { pane ->
                    pane?.viewModel?.state?.map { state ->
                        val title = state.tabTitle()
                        if (tree.count > 1) BetterLogcatBundle.message("tab.title.split", title, tree.count - 1) else title
                    } ?: emptyFlow()
                }
                .distinctUntilChanged()
                .collect { title -> withContext(context = Dispatchers.EDT) { content.displayName = title } }
        }

        toolWindow.contentManager.addContent(content)
        toolWindow.contentManager.setSelectedContent(content)
        saveTabs()
    }

    private fun saveTabs() {
        if (project.isDisposed || toolWindow.isDisposed) return
        val contents = toolWindow.contentManager.contents
        layoutStore.state.tabs = contents.mapNotNull { trees[it]?.snapshot()?.encode() }.toMutableList()
        layoutStore.state.selectedTab = contents.indexOf(toolWindow.contentManager.selectedContent).coerceAtLeast(minimumValue = 0)
        layoutStore.state.isSaved = true
    }

    private companion object {
        const val SAVE_INTERVAL_MILLIS = 3_000L
    }

    /** Shown when every tab is closed: the tool window stays open with a link to a new tab. */
    fun setUpEmptyState() {
        val toolWindowEx = toolWindow as? ToolWindowEx ?: return
        toolWindowEx.setToHideOnEmptyContent(false)
        toolWindowEx.emptyText
            ?.setText(BetterLogcatBundle.message("tabs.empty"))
            ?.appendLine(BetterLogcatBundle.message("tab.new"), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) { addTab() }
    }
}
