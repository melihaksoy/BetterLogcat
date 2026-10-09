package com.melihaksoy.betterlogcat.ide

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Short display names for long packages, for example `com.example.app.debug` → `Example debug`.
 * Visual only: filters, copy and export keep the real package. One store, so every pane updates at once.
 */
@Service(Service.Level.APP)
class PackageAliasStore {

    private val settings = BetterLogcatSettings.getInstance()
    private val _aliases = MutableStateFlow(value = settings.state.packageAliases.toMap())
    val aliases: StateFlow<Map<String, String>> = _aliases.asStateFlow()

    /** A blank [alias] removes the alias. */
    fun set(
        packageName: String,
        alias: String,
    ) {
        val trimmed = alias.trim()
        val updated = settings.state.packageAliases.toMutableMap()
        if (trimmed.isEmpty()) updated.remove(packageName) else updated[packageName] = trimmed
        // Replaced, not changed in place, so the settings store sees the change.
        settings.state.packageAliases = updated
        _aliases.value = updated.toMap()
    }

    companion object {
        fun getInstance(): PackageAliasStore = service()

        /** Display text for a process name. Keeps a `:service` suffix: `com.example.app:sync` → `Example:sync`. */
        fun display(
            processName: String,
            aliases: Map<String, String>,
        ): String {
            if (aliases.isEmpty()) return processName
            val packageName = processName.substringBefore(':')
            val alias = aliases[packageName] ?: return processName
            val suffix = processName.removePrefix(packageName)
            return alias + suffix
        }
    }
}
