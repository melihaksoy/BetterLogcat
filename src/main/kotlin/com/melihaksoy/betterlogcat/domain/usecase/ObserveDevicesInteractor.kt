package com.melihaksoy.betterlogcat.domain.usecase

import com.melihaksoy.betterlogcat.core.EitherFlow
import com.melihaksoy.betterlogcat.model.Device

/** Emits the connected device list each time it changes. */
interface ObserveDevicesInteractor {
    operator fun invoke(): EitherFlow<List<Device>>
}
