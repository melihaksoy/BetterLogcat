package com.melihaksoy.betterlogcat.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.melihaksoy.betterlogcat.BetterLogcatBundle

/**
 * Creates the "Better Logcat" tool window with one tab and a "New Tab" header action.
 * The platform registers an "Activate Better Logcat Window" action, so users can bind a key in Settings | Keymap.
 */
class BetterLogcatToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        val tabs = BetterLogcatTabs(
            project = project,
            toolWindow = toolWindow,
        )
        val newTab = DumbAwareAction.create(
            BetterLogcatBundle.message("tab.new"),
            AllIcons.General.Add,
        ) { tabs.addTab() }
        // Tab actions sit right after the last tab, like the Terminal "+".
        val toolWindowEx = toolWindow as? ToolWindowEx
        if (toolWindowEx != null) toolWindowEx.setTabActions(newTab) else toolWindow.setTitleActions(listOf(newTab))
        tabs.setUpEmptyState()
        tabs.addTab()
    }
}
