package com.melihaksoy.betterlogcat.interactor

import com.melihaksoy.betterlogcat.adb.AdbSession
import com.melihaksoy.betterlogcat.core.Either
import com.melihaksoy.betterlogcat.core.EitherFlow
import com.melihaksoy.betterlogcat.core.eitherFlow
import com.melihaksoy.betterlogcat.domain.usecase.TakeScreenshotInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.imageio.ImageIO

class TakeScreenshotInteractorImpl(private val session: AdbSession) : TakeScreenshotInteractor {

    override fun invoke(serial: String): EitherFlow<BufferedImage> = eitherFlow<BufferedImage> {
        val png = session.client().runBytes(args = listOf("-s", serial, "exec-out", "screencap", "-p"))
        val image = ImageIO.read(ByteArrayInputStream(png)) ?: throw IOException("Device returned no image")
        emit(Either.Right(value = image))
    }.flowOn(context = Dispatchers.IO)
}
