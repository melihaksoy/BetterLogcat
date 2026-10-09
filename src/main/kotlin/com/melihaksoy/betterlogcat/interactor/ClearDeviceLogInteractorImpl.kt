package com.melihaksoy.betterlogcat.interactor

import com.melihaksoy.betterlogcat.adb.AdbSession
import com.melihaksoy.betterlogcat.core.Either
import com.melihaksoy.betterlogcat.core.EitherFlow
import com.melihaksoy.betterlogcat.core.eitherFlow
import com.melihaksoy.betterlogcat.domain.usecase.ClearDeviceLogInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

class ClearDeviceLogInteractorImpl(private val session: AdbSession) : ClearDeviceLogInteractor {

    override fun invoke(serial: String): EitherFlow<Unit> = eitherFlow<Unit> {
        session.client().run(args = listOf("-s", serial, "logcat", "-c"))
        emit(Either.Right(value = Unit))
    }.flowOn(context = Dispatchers.IO)
}
