package com.melihaksoy.betterlogcat.media

import com.melihaksoy.betterlogcat.adb.AdbClient
import com.melihaksoy.betterlogcat.adb.AdbSession
import com.melihaksoy.betterlogcat.model.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/** A recording in progress. [stop] ends it and returns the saved file on this computer. */
interface ScreenRecording {
    val limitSeconds: Int

    /** True when the recorder stopped by itself, for example at the time limit. */
    val isFinished: Boolean

    suspend fun stop(): File
}

/**
 * Two recorders, like Android Studio:
 * - Emulator: the emulator writes WebM straight to this computer (`adb emu screenrecord`).
 * - Device: `screenrecord` writes MP4 on the device. SIGINT ends it cleanly, then the file is pulled.
 */
class ScreenRecorder(private val session: AdbSession) {

    suspend fun start(
        device: Device,
        options: RecordOptions,
    ): ScreenRecording = withContext(context = Dispatchers.IO) {
        val client = session.client()
        val name = fileName(device = device)
        options.directory.mkdirs()
        val size = scaledSize(
            client = client,
            serial = device.serial,
            percent = options.scalePercent,
        )
        if (device.isEmulator && options.useEmulatorRecording) {
            startEmulator(
                client = client,
                device = device,
                options = options,
                size = size,
                target = File(options.directory, "$name.webm"),
            )
        } else {
            startDevice(
                client = client,
                device = device,
                options = options,
                size = size,
                target = File(options.directory, "$name.mp4"),
            )
        }
    }

    private suspend fun startEmulator(
        client: AdbClient,
        device: Device,
        options: RecordOptions,
        size: String,
        target: File,
    ): ScreenRecording {
        val args = buildList {
            addAll(listOf("-s", device.serial, "emu", "screenrecord", "start", "--time-limit", EMULATOR_LIMIT.toString()))
            addAll(listOf("--bit-rate", (options.bitRateMbps * MEGA).toString()))
            if (size.isNotEmpty()) addAll(listOf("--size", size))
            add(target.absolutePath)
        }
        val reply = client.run(args = args)
        if (!reply.contains("OK")) throw IOException(reply.trim().ifEmpty { "Emulator did not start recording" })
        val startedAt = System.currentTimeMillis()
        return object : ScreenRecording {
            override val limitSeconds: Int = EMULATOR_LIMIT

            override val isFinished: Boolean
                get() = System.currentTimeMillis() - startedAt > EMULATOR_LIMIT * 1_000L

            override suspend fun stop(): File = withContext(context = Dispatchers.IO) {
                client.run(args = listOf("-s", device.serial, "emu", "screenrecord", "stop"))
                waitForFile(file = target)
                target
            }
        }
    }

    private suspend fun startDevice(
        client: AdbClient,
        device: Device,
        options: RecordOptions,
        size: String,
        target: File,
    ): ScreenRecording {
        val serial = device.serial
        val remote = "/sdcard/${target.name}"
        val help = client.run(args = listOf("-s", serial, "shell", "screenrecord", "--help"))
        // Newer screenrecord accepts 0 ("no limit"); older versions cap the time limit at 180 seconds.
        val limit = if (help.contains("Set to 0")) LONG_DEVICE_LIMIT else SHORT_DEVICE_LIMIT
        val oldShowTouches = if (options.showTaps) {
            client.run(args = listOf("-s", serial, "shell", "settings", "get", "system", "show_touches")).trim()
        } else {
            ""
        }
        if (options.showTaps) client.run(args = listOf("-s", serial, "shell", "settings", "put", "system", "show_touches", "1"))

        val args = buildList {
            addAll(listOf("-s", serial, "shell", "screenrecord"))
            addAll(listOf("--bit-rate", (options.bitRateMbps * MEGA).toString()))
            addAll(listOf("--time-limit", limit.toString()))
            if (size.isNotEmpty()) addAll(listOf("--size", size))
            add(remote)
        }
        val process = client.start(args = args)
        return object : ScreenRecording {
            override val limitSeconds: Int = limit

            override val isFinished: Boolean
                get() = !process.isAlive

            override suspend fun stop(): File = withContext(context = Dispatchers.IO) {
                try {
                    if (process.isAlive) {
                        client.run(args = listOf("-s", serial, "shell", "pkill", "-INT", "screenrecord"))
                        runInterruptible { process.waitFor(15, TimeUnit.SECONDS) }
                    }
                    // screenrecord finishes the MP4 after it exits; give the file system a moment.
                    delay(duration = 500.milliseconds)
                    client.run(args = listOf("-s", serial, "pull", remote, target.absolutePath))
                    client.run(args = listOf("-s", serial, "shell", "rm", "-f", remote))
                    if (!target.isFile) throw IOException("Could not pull $remote")
                    target
                } finally {
                    if (options.showTaps) {
                        val restore = oldShowTouches.ifEmpty { "0" }.takeIf { it == "0" || it == "1" } ?: "0"
                        client.run(args = listOf("-s", serial, "shell", "settings", "put", "system", "show_touches", restore))
                    }
                }
            }
        }
    }

    /** "WIDTHxHEIGHT" at [percent] of the screen, both even (video encoders need even sizes). Empty at 100 %. */
    private suspend fun scaledSize(
        client: AdbClient,
        serial: String,
        percent: Int,
    ): String {
        if (percent >= 100) return ""
        val output = client.run(args = listOf("-s", serial, "shell", "wm", "size"))
        val match = Regex(pattern = """(?:Override|Physical) size: (\d+)x(\d+)""").findAll(input = output).lastOrNull() ?: return ""
        val width = match.groupValues[1].toInt() * percent / 100 / 2 * 2
        val height = match.groupValues[2].toInt() * percent / 100 / 2 * 2
        return "${width}x$height"
    }

    private suspend fun waitForFile(file: File) {
        repeat(times = 20) {
            if (file.isFile && file.length() > 0) return
            delay(duration = 250.milliseconds)
        }
        throw IOException("Recording file not found: ${file.absolutePath}")
    }

    companion object {
        private const val MEGA = 1_000_000
        private const val EMULATOR_LIMIT = 1_800
        private const val LONG_DEVICE_LIMIT = 1_800
        private const val SHORT_DEVICE_LIMIT = 180

        fun fileName(device: Device): String {
            val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val safeName = device.name.replace(
                regex = Regex(pattern = "[^A-Za-z0-9._-]+"),
                replacement = "_",
            )
            return "${safeName}_$time"
        }
    }
}
