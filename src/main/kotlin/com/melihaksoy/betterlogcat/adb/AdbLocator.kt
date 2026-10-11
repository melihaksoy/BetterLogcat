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
        sdkRoots().forEach { yield(adbIn(sdk = it)) }
        PathEnvironmentVariableUtil.findInPath(executableName())?.let { yield(it) }
    }

    /** Android SDK folders that may exist: project local.properties, env vars, then the default folder. */
    fun sdkRoots(): List<File> = buildList {
        localPropertiesSdk()?.let { add(it) }
        listOf("ANDROID_HOME", "ANDROID_SDK_ROOT")
            .mapNotNull { System.getenv(it) }
            .filter { it.isNotBlank() }
            .forEach { add(File(it)) }
        val home = System.getProperty("user.home")
        when {
            SystemInfo.isMac -> add(File(home, "Library/Android/sdk"))
            SystemInfo.isWindows -> System.getenv("LOCALAPPDATA")?.let { add(File(it, "Android/Sdk")) }
            else -> add(File(home, "Android/Sdk"))
        }
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
