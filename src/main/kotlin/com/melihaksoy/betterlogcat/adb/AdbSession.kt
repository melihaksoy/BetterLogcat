package com.melihaksoy.betterlogcat.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class AdbNotFoundException : Exception("adb executable not found")

/** Locates adb on each call, so a fixed settings path takes effect without a restart. Starts the server once per path. */
class AdbSession(private val locator: AdbLocator) {

    private val mutex = Mutex()
    private var startedFor: File? = null

    suspend fun client(): AdbClient {
        val adb = withContext(context = Dispatchers.IO) { locator.locate() } ?: throw AdbNotFoundException()
        val client = AdbClient(adb = adb)
        mutex.withLock {
            if (startedFor != adb) {
                client.startServer()
                startedFor = adb
            }
        }
        return client
    }
}
