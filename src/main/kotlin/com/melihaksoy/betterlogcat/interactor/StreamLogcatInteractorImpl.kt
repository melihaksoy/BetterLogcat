package com.melihaksoy.betterlogcat.interactor

import com.melihaksoy.betterlogcat.adb.AdbSession
import com.melihaksoy.betterlogcat.adb.LogcatBinaryParser
import com.melihaksoy.betterlogcat.core.Either
import com.melihaksoy.betterlogcat.core.EitherFlow
import com.melihaksoy.betterlogcat.core.eitherFlow
import com.melihaksoy.betterlogcat.domain.usecase.LogcatRequest
import com.melihaksoy.betterlogcat.domain.usecase.StreamLogcatInteractor
import com.melihaksoy.betterlogcat.model.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.atomic.AtomicLong

class StreamLogcatInteractorImpl(private val session: AdbSession) : StreamLogcatInteractor {

    private val ids = AtomicLong()

    override fun invoke(request: LogcatRequest): EitherFlow<List<LogEntry>> = eitherFlow<List<LogEntry>> {
        val client = session.client()
        val parser = LogcatBinaryParser(nextId = { ids.incrementAndGet() })
        client.stream(args = arguments(request = request)).collect { chunk ->
            val entries = parser.feed(chunk = chunk)
            if (entries.isNotEmpty()) emit(Either.Right(value = entries))
        }
    }.flowOn(context = Dispatchers.IO)

    private fun arguments(request: LogcatRequest): List<String> {
        val base = listOf("-s", request.serial, "exec-out", "logcat", "-B")
        return when {
            request.sinceEpochNanos > 0 -> base + listOf("-T", formatSince(epochNanos = request.sinceEpochNanos))
            request.sinceEpochNanos < 0 -> base + listOf("-T", "1")
            else -> base
        }
    }

    /** logcat accepts `-T sssss.nnnnnnnnn` (epoch seconds). */
    private fun formatSince(epochNanos: Long): String {
        val seconds = epochNanos / NANOS_PER_SECOND
        val nanos = epochNanos % NANOS_PER_SECOND
        return "$seconds.${nanos.toString().padStart(length = 9, padChar = '0')}"
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
