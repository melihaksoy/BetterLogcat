package com.melihaksoy.betterlogcat.ui.media

import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.JComponent
import kotlin.math.min

/** Draws an image scaled to fit, centered, aspect ratio kept. */
internal class ImagePanel : JComponent() {

    var image: BufferedImage? = null
        set(value) {
            field = value
            repaint()
        }

    override fun getPreferredSize(): Dimension = JBUI.size(380, 640)

    override fun paintComponent(graphics: Graphics) {
        val current = image ?: return
        val g2 = graphics.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            val scale = min(width.toDouble() / current.width, height.toDouble() / current.height)
            val drawWidth = (current.width * scale).toInt()
            val drawHeight = (current.height * scale).toInt()
            g2.drawImage(current, (width - drawWidth) / 2, (height - drawHeight) / 2, drawWidth, drawHeight, null)
        } finally {
            g2.dispose()
        }
    }
}
