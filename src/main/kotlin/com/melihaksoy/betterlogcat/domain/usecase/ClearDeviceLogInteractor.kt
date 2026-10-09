package com.melihaksoy.betterlogcat.domain.usecase

import com.melihaksoy.betterlogcat.core.EitherFlow

/** Runs `logcat -c` on the device. */
interface ClearDeviceLogInteractor {
    operator fun invoke(serial: String): EitherFlow<Unit>
}
