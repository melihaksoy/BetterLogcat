package com.melihaksoy.betterlogcat.ui.viewsettings

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.ColorChooserService
import java.awt.Color
import java.awt.Point
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.util.ui.JBUI
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.model.HighlightPreset
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.LogcatState
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.jewel.bridge.JewelComposePanel
import javax.swing.Action
import javax.swing.JComponent

/**
 * Separate window for view settings. Modeless, so the log stays usable behind it and every change
 * shows in the pane at once. One window per pane; opening it again brings it to front.
 */
class ViewSettingsDialog(
    project: Project,
    private val state: StateFlow<LogcatState>,
    private val onIntent: (LogcatIntent) -> Unit,
    private val onOpenIdeSettings: () -> Unit,
    private val onEditAlias: (String) -> Unit,
    private val onAddAlias: () -> Unit,
) : DialogWrapper(project, null, true, IdeModalityType.MODELESS) {

    private val dialogProject = project
    private var composePanel: JComponent? = null

    init {
        title = BetterLogcatBundle.message("viewSettings.title")
        setOKButtonText(BetterLogcatBundle.message("viewSettings.done"))
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JewelComposePanel {
            val current by state.collectAsState()
            ViewSettingsContent(
                viewOptions = current.viewOptions,
                mappingFileName = current.mappingFileName,
                packageAliases = current.packageAliases,
                levelColors = current.levelColors,
                highlightRules = current.highlightRules,
                onIntent = onIntent,
                onOpenIdeSettings = onOpenIdeSettings,
                onEditAlias = onEditAlias,
                onAddAlias = onAddAlias,
                onPickColor = ::pickColor,
                onPickPreset = ::pickPreset,
            )
        }
        panel.preferredSize = JBUI.size(940, 760)
        composePanel = panel
        return panel
    }

    override fun createActions(): Array<Action> = arrayOf(okAction)

    /** Opens the IDE color picker. Compose colors are ARGB longs; AWT colors are ARGB ints. */
    private fun pickColor(
        initial: Long,
        onPicked: (Long) -> Unit,
    ) {
        ApplicationManager.getApplication().invokeLater {
            val picked = ColorChooserService.getInstance().showDialog(
                dialogProject,
                contentPanel,
                BetterLogcatBundle.message("colors.pick"),
                Color(initial.toInt(), true),
            ) ?: return@invokeLater
            onPicked(picked.rgb.toLong() and 0xFFFFFFFFL)
        }
    }

    /** An IDE list popup under the presets dropdown, the same kind as the pickers in the tool window. */
    private fun pickPreset(
        at: Point,
        presets: List<HighlightPreset>,
        onPicked: (HighlightPreset) -> Unit,
    ) {
        val owner = composePanel ?: return
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(presets)
            .setRenderer(textListCellRenderer { preset: HighlightPreset -> BetterLogcatBundle.message(preset.nameKey) })
            .setItemChosenCallback { onPicked(it) }
            .createPopup()
            .show(RelativePoint(owner, at))
    }

    override fun getDimensionServiceKey(): String = "BetterLogcat.ViewSettings"
}
