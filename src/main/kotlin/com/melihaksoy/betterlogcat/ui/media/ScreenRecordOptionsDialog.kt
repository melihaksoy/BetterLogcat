package com.melihaksoy.betterlogcat.ui.media

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.media.MediaFiles
import com.melihaksoy.betterlogcat.media.RecordOptions
import com.melihaksoy.betterlogcat.model.Device
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import javax.swing.JComponent

/** Like Android Studio's "Screen Recorder Options". Values persist for the next recording. */
class ScreenRecordOptionsDialog(
    private val project: Project,
    private val device: Device,
) : DialogWrapper(project, true) {

    private val state = BetterLogcatSettings.getInstance().state

    init {
        title = BetterLogcatBundle.message("record.options.title")
        setOKButtonText(BetterLogcatBundle.message("record.start"))
        init()
    }

    val options: RecordOptions
        get() = RecordOptions(
            bitRateMbps = state.recordBitRateMbps,
            scalePercent = state.recordScale,
            showTaps = state.recordShowTaps,
            useEmulatorRecording = device.isEmulator && state.recordUseEmulator,
            directory = MediaFiles.directory(),
        )

    override fun createCenterPanel(): JComponent = panel {
        row { label(BetterLogcatBundle.message("record.limitHint")) }
        row(BetterLogcatBundle.message("record.bitRate")) {
            intTextField(range = 1..100)
                .bindIntText(
                    getter = { state.recordBitRateMbps },
                    setter = { state.recordBitRateMbps = it },
                )
        }
        row(BetterLogcatBundle.message("media.resolution")) {
            comboBox(listOf(100, 75, 50, 33, 25))
                .bindItem(
                    getter = { state.recordScale },
                    setter = { state.recordScale = it ?: 100 },
                )
        }
        row {
            checkBox(BetterLogcatBundle.message("record.showTaps"))
                .bindSelected(
                    getter = { state.recordShowTaps },
                    setter = { state.recordShowTaps = it },
                )
                .comment(BetterLogcatBundle.message("record.showTaps.comment"))
        }
        row {
            checkBox(BetterLogcatBundle.message("record.useEmulator"))
                .bindSelected(
                    getter = { state.recordUseEmulator },
                    setter = { state.recordUseEmulator = it },
                )
                .enabled(device.isEmulator)
                .comment(BetterLogcatBundle.message("record.useEmulator.comment"))
        }
        mediaDirectoryRow(project = project)
    }
}
