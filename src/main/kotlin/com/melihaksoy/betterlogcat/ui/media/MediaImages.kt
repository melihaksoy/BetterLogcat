package com.melihaksoy.betterlogcat.ui.media

import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

internal object MediaImages {

    /** Rotates by 90 degrees: clockwise when [isClockwise], else counterclockwise. */
    fun rotate(
        image: BufferedImage,
        isClockwise: Boolean,
    ): BufferedImage {
        val rotated = BufferedImage(image.height, image.width, BufferedImage.TYPE_INT_ARGB)
        val g2 = rotated.createGraphics()
        val transform = AffineTransform()
        if (isClockwise) {
            transform.translate(image.height.toDouble(), 0.0)
            transform.rotate(Math.PI / 2)
        } else {
            transform.translate(0.0, image.width.toDouble())
            transform.rotate(-Math.PI / 2)
        }
        g2.drawImage(image, transform, null)
        g2.dispose()
        return rotated
    }

    fun scale(
        image: BufferedImage,
        percent: Int,
    ): BufferedImage {
        if (percent >= 100) return image
        val width = (image.width * percent / 100).coerceAtLeast(minimumValue = 1)
        val height = (image.height * percent / 100).coerceAtLeast(minimumValue = 1)
        val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2 = scaled.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2.drawImage(image, 0, 0, width, height, null)
        g2.dispose()
        return scaled
    }
}
