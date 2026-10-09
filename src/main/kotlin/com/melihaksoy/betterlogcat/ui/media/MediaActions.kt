package com.melihaksoy.betterlogcat.ui.media

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.ide.BetterLogcatProjectService
import com.melihaksoy.betterlogcat.interactor.TakeScreenshotInteractorImpl
import com.melihaksoy.betterlogcat.media.MediaFiles
import com.melihaksoy.betterlogcat.media.ScreenRecorder
import com.melihaksoy.betterlogcat.model.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Opens the screenshot and screen record flows for a device. Call on the EDT. */
object MediaActions {

    fun takeScreenshot(
        project: Project,
        device: Device,
    ) {
        val service = project.service<BetterLogcatProjectService>()
        ScreenshotDialog(
            project = project,
            device = device,
            scope = service.scope,
            takeScreenshot = TakeScreenshotInteractorImpl(session = service.adbSession),
        ).show()
    }

    fun recordScreen(
        project: Project,
        device: Device,
    ) {
        val optionsDialog = ScreenRecordOptionsDialog(
            project = project,
            device = device,
        )
        if (!optionsDialog.showAndGet()) return
        val options = optionsDialog.options
        val service = project.service<BetterLogcatProjectService>()
        service.scope.launch {
            val recording = runCatching {
                ScreenRecorder(session = service.adbSession).start(
                    device = device,
                    options = options,
                )
            }
            withContext(context = Dispatchers.EDT) {
                recording
                    .onSuccess {
                        ScreenRecordingDialog(
                            project = project,
                            device = device,
                            scope = service.scope,
                            recording = it,
                        ).show()
                    }
                    .onFailure { MediaFiles.notifyError(project = project, message = BetterLogcatBundle.message("record.failed", it.message.orEmpty())) }
            }
        }
    }
}
