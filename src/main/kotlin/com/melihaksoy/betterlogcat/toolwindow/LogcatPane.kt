package com.melihaksoy.betterlogcat.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.melihaksoy.betterlogcat.ide.BetterLogcatProjectService
import com.melihaksoy.betterlogcat.ide.AppearanceStore
import com.melihaksoy.betterlogcat.ide.PackageAliasStore
import com.melihaksoy.betterlogcat.ide.SourceNavigator
import com.melihaksoy.betterlogcat.interactor.ClearDeviceLogInteractorImpl
import com.melihaksoy.betterlogcat.interactor.ObserveProcessesInteractorImpl
import com.melihaksoy.betterlogcat.interactor.StreamLogcatInteractorImpl
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import com.melihaksoy.betterlogcat.ui.logcat.LogcatScreen
import com.melihaksoy.betterlogcat.ui.logcat.LogcatViewModel
import com.melihaksoy.betterlogcat.ui.logcat.pane.PaneActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import org.jetbrains.jewel.bridge.JewelComposePanel
import javax.swing.JComponent

/** One log view: a ViewModel and its Compose panel. Disposing it stops its stream. */
class LogcatPane(
    project: Project,
    service: BetterLogcatProjectService,
    initialSerial: String,
    initialQuery: String?,
    actions: (LogcatPane) -> PaneActions,
) : Disposable {

    private val job = SupervisorJob(parent = service.scope.coroutineContext.job)
    val scope = CoroutineScope(context = service.scope.coroutineContext + job)

    val viewModel = LogcatViewModel(
        scope = scope,
        initialSerial = initialSerial,
        initialQuery = initialQuery,
        observeDevices = service.observeDevices,
        observeProjectAppIds = service.observeProjectAppIds,
        streamLogcat = StreamLogcatInteractorImpl(session = service.adbSession),
        observeProcesses = ObserveProcessesInteractorImpl(session = service.adbSession),
        clearDeviceLog = ClearDeviceLogInteractorImpl(session = service.adbSession),
        sourceNavigator = SourceNavigator(project = project),
        settings = BetterLogcatSettings.getInstance(),
        aliasStore = PackageAliasStore.getInstance(),
        appearanceStore = AppearanceStore.getInstance(),
    )

    private val paneActions = actions(this)

    val component: JComponent = JewelComposePanel(focusOnClickInside = true) {
        LogcatScreen(
            viewModel = viewModel,
            project = project,
            paneActions = paneActions,
        )
    }

    override fun dispose() {
        job.cancel()
    }
}
