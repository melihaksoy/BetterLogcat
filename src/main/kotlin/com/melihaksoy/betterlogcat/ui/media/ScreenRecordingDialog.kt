package com.melihaksoy.betterlogcat.ui.media

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.panel
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.media.MediaFiles
import com.melihaksoy.betterlogcat.media.ScreenRecording
import com.melihaksoy.betterlogcat.model.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.Timer

/** Small window that counts up while recording, like Android Studio's "Record Screen". Closing it also stops. */
class ScreenRecordingDialog(
    private val project: Project,
    device: Device,
    private val scope: CoroutineScope,
    private val recording: ScreenRecording,
) : DialogWrapper(project, false, IdeModalityType.MODELESS) {

    private val startedAt = System.currentTimeMillis()
    private val statusLabel = JBLabel(BetterLogcatBundle.message("record.elapsed", "00:00"))
    private val timer = Timer(TICK_MILLIS) { tick() }
    private var isStopping = false

    private val stopAction = object : AbstractAction(BetterLogcatBundle.message("record.stop")) {
        override fun actionPerformed(event: ActionEvent) = stop()
    }

    init {
        title = BetterLogcatBundle.message("record.title", device.name)
        init()
        timer.start()
    }

    override fun createCenterPanel(): JComponent = panel {
        row { cell(statusLabel) }
    }

    override fun createActions(): Array<Action> = arrayOf(stopAction)

    override fun doCancelAction() = stop()

    private fun tick() {
        val seconds = (System.currentTimeMillis() - startedAt) / 1_000
        statusLabel.text = BetterLogcatBundle.message("record.elapsed", "%02d:%02d".format(seconds / 60, seconds % 60))
        if (recording.isFinished) stop()
    }

    private fun stop() {
        if (isStopping) return
        isStopping = true
        timer.stop()
        stopAction.isEnabled = false
        statusLabel.text = BetterLogcatBundle.message("record.saving")
        scope.launch {
            val result = runCatching { recording.stop() }
            withContext(context = Dispatchers.EDT) {
                close(OK_EXIT_CODE)
                result
                    .onSuccess { MediaFiles.notifySaved(project = project, file = it) }
                    .onFailure { MediaFiles.notifyError(project = project, message = BetterLogcatBundle.message("record.failed", it.message.orEmpty())) }
            }
        }
    }

    private companion object {
        const val TICK_MILLIS = 500
    }
}
