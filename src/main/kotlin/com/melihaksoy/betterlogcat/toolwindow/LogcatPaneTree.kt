package com.melihaksoy.betterlogcat.toolwindow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.melihaksoy.betterlogcat.ide.BetterLogcatProjectService
import com.melihaksoy.betterlogcat.ui.logcat.pane.PaneActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.BorderLayout
import java.awt.Container
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * The panes of one tab, laid out in nested splitters. A split puts a new pane next to its source;
 * closing a pane gives its space back to its sibling.
 */
class LogcatPaneTree(
    private val project: Project,
    private val service: BetterLogcatProjectService,
    initialSerial: String,
) : Disposable {

    val component: JPanel = JPanel(BorderLayout())

    private val panes = ArrayList<LogcatPane>()
    private var paneCount by mutableStateOf(value = 0)

    private val _primary = MutableStateFlow<LogcatPane?>(value = null)

    /** The pane that names the tab: the oldest open pane. */
    val primary: StateFlow<LogcatPane?> = _primary.asStateFlow()

    val count: Int
        get() = panes.size

    init {
        val first = createPane(
            serial = initialSerial,
            query = null,
        )
        component.add(first.component, BorderLayout.CENTER)
    }

    /** [query] null restores the user's last filter. */
    private fun createPane(
        serial: String,
        query: String?,
    ): LogcatPane {
        val pane = LogcatPane(
            project = project,
            service = service,
            initialSerial = serial,
            initialQuery = query,
            actions = { pane -> actionsFor(pane = pane) },
        )
        Disposer.register(this, pane)
        panes += pane
        paneCount = panes.size
        if (_primary.value == null) _primary.value = pane
        return pane
    }

    private fun actionsFor(pane: LogcatPane): PaneActions = object : PaneActions {
        override val canClose: Boolean
            get() = paneCount > 1

        override fun splitRight() = split(
            source = pane,
            isVertical = false,
        )

        override fun splitDown() = split(
            source = pane,
            isVertical = true,
        )

        override fun close() = close(pane = pane)
    }

    /** [isVertical] stacks the panes top and bottom; false puts them side by side. */
    private fun split(
        source: LogcatPane,
        isVertical: Boolean,
    ) {
        val sourceState = source.viewModel.state.value
        val newPane = createPane(
            serial = sourceState.selectedSerial,
            query = sourceState.filter.toQueryString(),
        )
        val splitter = OnePixelSplitter(isVertical, 0.5f)
        replace(
            old = source.component,
            new = splitter,
        )
        splitter.firstComponent = source.component
        splitter.secondComponent = newPane.component
        refresh()
    }

    private fun close(pane: LogcatPane) {
        if (panes.size <= 1) return
        val splitter = pane.component.parent as? OnePixelSplitter ?: return
        val sibling = if (splitter.firstComponent === pane.component) splitter.secondComponent else splitter.firstComponent
        splitter.firstComponent = null
        splitter.secondComponent = null
        replace(
            old = splitter,
            new = sibling,
        )
        panes -= pane
        paneCount = panes.size
        if (_primary.value === pane) _primary.value = panes.firstOrNull()
        Disposer.dispose(pane)
        refresh()
    }

    private fun replace(
        old: JComponent,
        new: JComponent,
    ) {
        when (val parent: Container? = old.parent) {
            is OnePixelSplitter -> if (parent.firstComponent === old) parent.firstComponent = new else parent.secondComponent = new
            else -> {
                component.remove(old)
                component.add(new, BorderLayout.CENTER)
            }
        }
    }

    private fun refresh() {
        component.revalidate()
        component.repaint()
    }

    override fun dispose() = Unit
}
