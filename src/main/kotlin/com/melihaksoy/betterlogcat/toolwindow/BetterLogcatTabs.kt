package com.melihaksoy.betterlogcat.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.ide.BetterLogcatProjectService
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
                    reopenWhenEmpty()
                }
            },
        )
    }

    /** Adds a tab. It starts on the device of the selected tab. */
    fun addTab() {
        val selected = toolWindow.contentManager.selectedContent?.let { trees[it] }
        val initialSerial = selected?.primary?.value?.viewModel?.state?.value?.selectedSerial.orEmpty()

        val tree = LogcatPaneTree(
            project = project,
            service = service,
            initialSerial = initialSerial,
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
    }

    private fun reopenWhenEmpty() {
        ApplicationManager.getApplication().invokeLater(
            {
                if (!toolWindow.isDisposed && toolWindow.contentManager.contentCount == 0) addTab()
            },
            project.disposed,
        )
    }
}
