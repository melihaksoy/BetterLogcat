package com.melihaksoy.betterlogcat.domain.usecase

import com.melihaksoy.betterlogcat.core.EitherFlow

/** Emits the application ids of the open project. Empty until the build system knows them. */
interface ObserveProjectAppIdsInteractor {
    operator fun invoke(): EitherFlow<Set<String>>
}
