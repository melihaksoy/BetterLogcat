package com.melihaksoy.betterlogcat.ide

import com.melihaksoy.betterlogcat.adb.AdbSession
import com.melihaksoy.betterlogcat.format.AppVersion
import com.melihaksoy.betterlogcat.format.IssueReport
import kotlin.time.Duration.Companion.seconds

/** One-off adb commands outside the log stream: Wi-Fi pairing and app versions. */
class AdbCommands(private val session: AdbSession) {

    /** `adb pair host:port code`. Returns adb's answer, like "Successfully paired to …". */
    suspend fun pair(
        address: String,
        code: String,
    ): String = session.client().run(
        args = listOf("pair", address, code),
        timeout = PAIR_TIMEOUT,
    ).trim()

    /** `adb connect host:port`. Returns adb's answer, like "connected to …". */
    suspend fun connect(address: String): String = session.client().run(
        args = listOf("connect", address),
        timeout = PAIR_TIMEOUT,
    ).trim()

    /** The installed version of [packageName] on [serial], or null when adb cannot tell. */
    suspend fun appVersion(
        serial: String,
        packageName: String,
    ): AppVersion? = runCatching {
        IssueReport.parseVersion(
            dumpsys = session.client().run(args = listOf("-s", serial, "shell", "dumpsys", "package", packageName)),
        )
    }.getOrNull()

    private companion object {
        val PAIR_TIMEOUT = 20.seconds
    }
}
