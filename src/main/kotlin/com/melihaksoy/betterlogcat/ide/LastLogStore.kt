package com.melihaksoy.betterlogcat.ide

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.thisLogger
import com.melihaksoy.betterlogcat.format.SavedLogCodec
import com.melihaksoy.betterlogcat.model.SavedLog
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * The end of each device's log, one file per device, kept for the next IDE session. The files are in
 * the IDE system folder, never in the project, so no commit or shared project takes them.
 * Blocking file work: call it off the UI thread.
 */
class LastLogStore(private val directory: Path) {

    fun save(log: SavedLog) = synchronized(LOCK) {
        try {
            Files.createDirectories(directory)
            val temp = Files.createTempFile(directory, "last-log", ".tmp")
            try {
                GZIPOutputStream(Files.newOutputStream(temp)).use {
                    SavedLogCodec.write(
                        log = log,
                        output = it,
                    )
                }
                Files.move(temp, fileOf(serial = log.serial), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } finally {
                Files.deleteIfExists(temp)
            }
        } catch (exception: IOException) {
            thisLogger().warn("Could not save the last log of ${log.serial}", exception)
        }
    }

    /** Null when nothing is saved for [serial]. A file that cannot be read is deleted. */
    fun load(
        serial: String,
        firstId: Long,
    ): SavedLog? = synchronized(LOCK) {
        val file = fileOf(serial = serial)
        if (!Files.isRegularFile(file)) return null
        try {
            GZIPInputStream(Files.newInputStream(file)).buffered().use {
                SavedLogCodec.read(
                    input = it,
                    firstId = firstId,
                )
            }.takeIf { it.serial == serial }
        } catch (exception: IOException) {
            thisLogger().warn("Could not read the last log of $serial", exception)
            Files.deleteIfExists(file)
            null
        }
    }

    fun delete(serial: String) = synchronized(LOCK) {
        Files.deleteIfExists(fileOf(serial = serial))
    }

    fun deleteAll() = synchronized(LOCK) {
        if (!Files.isDirectory(directory)) return
        Files.list(directory).use { files -> files.forEach { Files.deleteIfExists(it) } }
    }

    // A serial can hold ':' (adb over Wi-Fi) and other characters a file name cannot.
    private fun fileOf(serial: String): Path = directory.resolve(serial.replace(Regex(pattern = "[^A-Za-z0-9._-]"), "_") + ".log.gz")

    companion object {
        /** Lines kept per device. The end of a run is what matters after a restart, and a small file loads fast. */
        const val MAX_ENTRIES = 10_000

        /** Every pane and the settings page share the files. */
        private val LOCK = Any()

        fun default(): LastLogStore = LastLogStore(directory = PathManager.getSystemDir().resolve("better-logcat").resolve("last-log"))
    }
}
