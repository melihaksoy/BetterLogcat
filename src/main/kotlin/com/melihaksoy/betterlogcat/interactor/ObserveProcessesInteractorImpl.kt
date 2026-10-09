package com.melihaksoy.betterlogcat.interactor

import com.melihaksoy.betterlogcat.adb.AdbSession
import com.melihaksoy.betterlogcat.adb.ProcessInfo
import com.melihaksoy.betterlogcat.adb.ProcessListParser
import com.melihaksoy.betterlogcat.core.Either
import com.melihaksoy.betterlogcat.core.EitherFlow
import com.melihaksoy.betterlogcat.domain.usecase.ObserveProcessesInteractor
import com.melihaksoy.betterlogcat.domain.usecase.ProcessSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlin.time.Duration.Companion.seconds

class ObserveProcessesInteractorImpl(private val session: AdbSession) : ObserveProcessesInteractor {

    override fun invoke(
        serial: String,
        refreshRequests: Flow<Unit>,
    ): EitherFlow<ProcessSnapshot> {
        val ticks = flow {
            while (true) {
                emit(Unit)
                delay(duration = POLL_INTERVAL)
            }
        }
        // Dead processes keep their names, so old entries still show a package.
        val known = HashMap<Int, String>()
        return merge(ticks, refreshRequests)
            .conflate()
            .map {
                try {
                    val running = load(serial = serial)
                    running.forEach { (pid, info) -> known[pid] = info.name }
                    val apps = running.values.filter { it.isApp }.map { it.name.substringBefore(':') }.toSortedSet()
                    Either.Right(
                        value = ProcessSnapshot(
                            names = HashMap(known),
                            appPackages = apps,
                        ),
                    )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Either.Left(value = exception)
                }
            }
            .flowOn(context = Dispatchers.IO)
    }

    private suspend fun load(serial: String): Map<Int, ProcessInfo> {
        val client = session.client()
        val modern = ProcessListParser.parse(
            output = client.run(args = listOf("-s", serial, "shell", "ps", "-A", "-o", "PID,USER,NAME")),
        )
        if (modern.isNotEmpty()) return modern
        return ProcessListParser.parse(output = client.run(args = listOf("-s", serial, "shell", "ps")))
    }

    private companion object {
        val POLL_INTERVAL = 3.seconds
    }
}
