package com.melihaksoy.betterlogcat.ui.media

import com.intellij.openapi.application.EDT
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.core.mapEither
import com.melihaksoy.betterlogcat.domain.usecase.TakeScreenshotInteractor
import com.melihaksoy.betterlogcat.media.MediaFiles
import com.melihaksoy.betterlogcat.media.ScreenRecorder
import com.melihaksoy.betterlogcat.model.Device
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/** Like Android Studio's screenshot dialog: recapture, rotate, copy, resolution and save folder. */
class ScreenshotDialog(
    private val project: Project,
    private val device: Device,
    private val scope: CoroutineScope,
    private val takeScreenshot: TakeScreenshotInteractor,
) : DialogWrapper(project, true) {

    private val imagePanel = ImagePanel()
    private val infoLabel = JBLabel()
    private val scaleBox = ComboBox(arrayOf(100, 75, 50, 33, 25))
    private var image: BufferedImage? = null

    init {
        title = BetterLogcatBundle.message("screenshot.title", device.name)
        setOKButtonText(BetterLogcatBundle.message("screenshot.save"))
        scaleBox.selectedItem = BetterLogcatSettings.getInstance().state.screenshotScale
        init()
        capture()
    }

    override fun createCenterPanel(): JComponent {
        val buttons = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0))
        buttons.add(button(text = BetterLogcatBundle.message("screenshot.recapture")) { capture() })
        buttons.add(button(text = BetterLogcatBundle.message("screenshot.rotateLeft")) { rotate(isClockwise = false) })
        buttons.add(button(text = BetterLogcatBundle.message("screenshot.rotateRight")) { rotate(isClockwise = true) })
        buttons.add(button(text = BetterLogcatBundle.message("screenshot.copy")) { copy() })

        val top = JPanel(BorderLayout())
        top.add(buttons, BorderLayout.WEST)
        top.add(infoLabel, BorderLayout.EAST)

        val bottom = panel {
            row(BetterLogcatBundle.message("media.resolution")) { cell(scaleBox) }
            mediaDirectoryRow(project = project)
        }

        val root = JPanel(BorderLayout(0, JBUI.scale(8)))
        root.add(top, BorderLayout.NORTH)
        root.add(imagePanel, BorderLayout.CENTER)
        root.add(bottom, BorderLayout.SOUTH)
        return root
    }

    override fun doOKAction() {
        val current = image ?: return
        val percent = scaleBox.selectedItem as? Int ?: 100
        BetterLogcatSettings.getInstance().state.screenshotScale = percent
        val target = File(MediaFiles.directory(), "Screenshot_${ScreenRecorder.fileName(device = device)}.png")
        super.doOKAction()
        scope.launch(context = Dispatchers.IO) {
            val isSaved = runCatching {
                target.parentFile.mkdirs()
                ImageIO.write(MediaImages.scale(image = current, percent = percent), "png", target)
            }.getOrDefault(false)
            withContext(context = Dispatchers.EDT) {
                if (isSaved) {
                    MediaFiles.notifySaved(project = project, file = target)
                } else {
                    MediaFiles.notifyError(project = project, message = BetterLogcatBundle.message("media.saveFailed", target.absolutePath))
                }
            }
        }
    }

    private fun capture() {
        infoLabel.text = BetterLogcatBundle.message("screenshot.capturing")
        isOKActionEnabled = false
        scope.launch {
            takeScreenshot(serial = device.serial)
                .mapEither(
                    onLeft = { error ->
                        withContext(context = Dispatchers.EDT) {
                            infoLabel.text = BetterLogcatBundle.message("screenshot.failed", error.message.orEmpty())
                        }
                    },
                    onRight = { captured -> withContext(context = Dispatchers.EDT) { show(image = captured) } },
                )
                .collect()
        }
    }

    private fun show(image: BufferedImage) {
        this.image = image
        imagePanel.image = image
        infoLabel.text = BetterLogcatBundle.message("screenshot.info", image.width, image.height)
        isOKActionEnabled = true
    }

    private fun rotate(isClockwise: Boolean) {
        val current = image ?: return
        show(
            image = MediaImages.rotate(
                image = current,
                isClockwise = isClockwise,
            ),
        )
    }

    private fun copy() {
        val current = image ?: return
        CopyPasteManager.getInstance().setContents(ImageTransferable(image = current))
        infoLabel.text = BetterLogcatBundle.message("screenshot.copied")
    }

    private fun button(
        text: String,
        onClick: () -> Unit,
    ): JButton = JButton(text).apply { addActionListener { onClick() } }
}
