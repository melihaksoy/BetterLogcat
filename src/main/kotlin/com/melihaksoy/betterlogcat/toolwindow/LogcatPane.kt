package com.melihaksoy.betterlogcat.toolwindow

import com.intellij.openapi.Disposable
import javax.swing.JPanel
import java.awt.datatransfer.StringSelection
import java.awt.BorderLayout
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.media.MediaFiles
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.intellij.ui.dsl.builder.panel
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.application.ApplicationManager
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationAction
import androidx.compose.ui.window.WindowExceptionHandler
import com.intellij.openapi.project.Project
import com.melihaksoy.betterlogcat.ide.BetterLogcatProjectService
import com.melihaksoy.betterlogcat.ide.AppearanceStore
import com.melihaksoy.betterlogcat.ide.BookmarkStore
import com.melihaksoy.betterlogcat.ide.PackageAliasStore
import com.melihaksoy.betterlogcat.ide.ProblemHistoryStore
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
import com.melihaksoy.betterlogcat.ide.TagNotesStore

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
        bookmarkStore = BookmarkStore.getInstance(project = project),
        tagNotesStore = TagNotesStore.getInstance(),
        problemHistoryStore = ProblemHistoryStore.getInstance(project = project),
    )

    private val paneActions = actions(this)

    private val project = project
    private val crashTimes = ArrayDeque<Long>()

    /** Stable holder, so the layout keeps its place while the Compose panel inside is rebuilt after a crash. */
    val component: JComponent = JPanel(BorderLayout())

    init {
        showLogs()
    }

    private fun showLogs() {
        component.removeAll()
        component.add(
            JewelComposePanel(
                focusOnClickInside = true,
                config = { exceptionHandler = WindowExceptionHandler { onCrash(throwable = it) } },
            ) {
                LogcatScreen(
                    viewModel = viewModel,
                    project = project,
                    paneActions = paneActions,
                )
            },
            BorderLayout.CENTER,
        )
        component.revalidate()
        component.repaint()
    }

    /**
     * A rendering error must not leave the pane dead. Reset the view to safe settings (a saved side
     * panel was what kept crashing) and rebuild. A second crash within a short time shows a Restart panel.
     */
    private fun onCrash(throwable: Throwable) {
        thisLogger().warn("Better Logcat pane crashed; rebuilding it", throwable)
        ApplicationManager.getApplication().invokeLater(
            {
                val now = System.currentTimeMillis()
                crashTimes.removeAll { now - it > CRASH_WINDOW_MILLIS }
                crashTimes.addLast(now)
                viewModel.onIntent(LogcatIntent.ResetViewAfterCrash)
                if (crashTimes.size >= 2) showCrashPanel(throwable = throwable) else showLogs()
                notifyCrash(throwable = throwable)
            },
            project.disposed,
        )
    }

    private fun showCrashPanel(throwable: Throwable) {
        component.removeAll()
        val details = panel {
            row { label(BetterLogcatBundle.message("crash.title")) }
            row { comment(throwable.toString()) }
            row {
                button(BetterLogcatBundle.message("crash.restart")) {
                    crashTimes.clear()
                    showLogs()
                }
                link(BetterLogcatBundle.message("crash.copy")) { copyDetails(throwable = throwable) }
            }
        }
        component.add(details, BorderLayout.CENTER)
        component.revalidate()
        component.repaint()
    }

    private fun notifyCrash(throwable: Throwable) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(MediaFiles.NOTIFICATION_GROUP)
            .createNotification(BetterLogcatBundle.message("crash.notification", throwable.javaClass.simpleName), NotificationType.WARNING)
            .addAction(NotificationAction.createSimple(BetterLogcatBundle.message("crash.copy")) { copyDetails(throwable = throwable) })
            .notify(project)
    }

    private fun copyDetails(throwable: Throwable) {
        CopyPasteManager.getInstance().setContents(StringSelection(throwable.stackTraceToString()))
    }

    override fun dispose() {
        job.cancel()
    }

    private companion object {
        const val CRASH_WINDOW_MILLIS = 10_000L
    }
}
