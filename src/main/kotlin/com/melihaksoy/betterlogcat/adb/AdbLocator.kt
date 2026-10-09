package com.melihaksoy.betterlogcat.adb

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import java.io.File
import java.util.Properties

/** Finds the adb executable. Order: settings, project local.properties, env vars, default SDK dirs, PATH. */
class AdbLocator(private val project: Project) {

    fun locate(): File? = candidates().firstOrNull { it.isFile && it.canExecute() }

    private fun candidates(): Sequence<File> = sequence {
        val configured = BetterLogcatSettings.getInstance().state.adbPath.orEmpty().trim()
        if (configured.isNotEmpty()) yield(File(configured))

        localPropertiesSdk()?.let { yield(adbIn(sdk = it)) }

        listOf("ANDROID_HOME", "ANDROID_SDK_ROOT")
            .mapNotNull { System.getenv(it) }
            .filter { it.isNotBlank() }
            .forEach { yield(adbIn(sdk = File(it))) }

        val home = System.getProperty("user.home")
        when {
            SystemInfo.isMac -> yield(adbIn(sdk = File(home, "Library/Android/sdk")))
            SystemInfo.isWindows -> System.getenv("LOCALAPPDATA")?.let { yield(adbIn(sdk = File(it, "Android/Sdk"))) }
            else -> yield(adbIn(sdk = File(home, "Android/Sdk")))
        }

        PathEnvironmentVariableUtil.findInPath(executableName())?.let { yield(it) }
    }

    private fun localPropertiesSdk(): File? {
        val basePath = project.basePath ?: return null
        val file = File(basePath, "local.properties")
        if (!file.isFile) return null
        val properties = Properties()
        file.inputStream().use { properties.load(it) }
        return properties.getProperty("sdk.dir")?.takeIf { it.isNotBlank() }?.let { File(it) }
    }

    private fun adbIn(sdk: File): File = File(File(sdk, "platform-tools"), executableName())

    private fun executableName(): String = if (SystemInfo.isWindows) "adb.exe" else "adb"
}
