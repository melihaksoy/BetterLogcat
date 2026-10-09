package com.melihaksoy.betterlogcat.adb

import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Thin wrapper around the adb executable. */
class AdbClient(private val adb: File) {

    /**
     * Starts the adb server with all output discarded. A server started by a later command
     * would inherit that command's stdout pipe and keep it open forever.
     */
    suspend fun startServer() {
        runInterruptible(context = Dispatchers.IO) {
            val process = ProcessBuilder(adb.path, "start-server")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            if (!process.waitFor(20, TimeUnit.SECONDS)) process.destroyForcibly()
        }
    }

    /** Runs one adb command and returns its stdout. */
    suspend fun run(
        args: List<String>,
        timeout: Duration = 10.seconds,
    ): String {
        val process = ProcessBuilder(listOf(adb.path) + args)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        val output = CompletableFuture.supplyAsync(
            { process.inputStream.bufferedReader().use { it.readText() } },
            AppExecutorUtil.getAppExecutorService(),
        )
        val text = withTimeoutOrNull(timeout = timeout) { output.await() }
        if (text == null) {
            process.destroyForcibly()
            throw IOException("adb ${args.joinToString(separator = " ")} timed out")
        }
        return text
    }

    /** Runs one adb command and returns its raw stdout, for binary output such as `screencap -p`. */
    suspend fun runBytes(
        args: List<String>,
        timeout: Duration = 20.seconds,
    ): ByteArray {
        val process = ProcessBuilder(listOf(adb.path) + args)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        val output = CompletableFuture.supplyAsync(
            { process.inputStream.use { it.readBytes() } },
            AppExecutorUtil.getAppExecutorService(),
        )
        val bytes = withTimeoutOrNull(timeout = timeout) { output.await() }
        if (bytes == null) {
            process.destroyForcibly()
            throw IOException("adb ${args.joinToString(separator = " ")} timed out")
        }
        return bytes
    }

    /** Starts a long-running adb command. The caller owns the process. */
    fun start(args: List<String>): Process = ProcessBuilder(listOf(adb.path) + args)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()

    /**
     * Streams raw bytes of a long-running adb command. Cancelling the collector kills the process,
     * which unblocks the reader thread.
     */
    fun stream(args: List<String>): Flow<ByteArray> = callbackFlow {
        val process = ProcessBuilder(listOf(adb.path) + args)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        val reader = Thread({
            val buffer = ByteArray(64 * 1024)
            try {
                process.inputStream.use { input ->
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        val result = trySendBlocking(element = buffer.copyOf(newSize = count))
                        if (result.isClosed) break
                    }
                }
                channel.close()
            } catch (exception: IOException) {
                channel.close(cause = exception)
            }
        }, "Better Logcat reader")
        reader.isDaemon = true
        reader.start()
        awaitClose { process.destroy() }
    }
}
