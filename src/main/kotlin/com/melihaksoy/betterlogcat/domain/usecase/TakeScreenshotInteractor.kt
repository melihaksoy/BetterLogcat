package com.melihaksoy.betterlogcat.domain.usecase

import com.melihaksoy.betterlogcat.core.EitherFlow
import java.awt.image.BufferedImage

/** Captures the device screen as an image. */
interface TakeScreenshotInteractor {
    operator fun invoke(serial: String): EitherFlow<BufferedImage>
}
