package com.melihaksoy.betterlogcat.domain.usecase

import com.melihaksoy.betterlogcat.core.EitherFlow
import kotlinx.coroutines.flow.Flow

/** Emits the processes of one device. Polls, and also refreshes on each [refreshRequests] signal. */
interface ObserveProcessesInteractor {
    operator fun invoke(
        serial: String,
        refreshRequests: Flow<Unit>,
    ): EitherFlow<ProcessSnapshot>
}

/** [names] maps pid to process name, dead processes included. [appPackages] are running app packages. */
data class ProcessSnapshot(
    val names: Map<Int, String>,
    val appPackages: Set<String>,
)
