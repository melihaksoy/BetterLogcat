package com.melihaksoy.betterlogcat.ide

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.melihaksoy.betterlogcat.model.HighlightRule
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User colors: level color overrides and keyword highlights. One store, so every pane updates at once. */
@Service(Service.Level.APP)
class AppearanceStore {

    private val settings = BetterLogcatSettings.getInstance()

    private val _levelColors = MutableStateFlow(value = loadLevelColors())
    val levelColors: StateFlow<Map<LogLevel, Long>> = _levelColors.asStateFlow()

    private val _highlightRules = MutableStateFlow(value = settings.state.highlightRules.mapNotNull { HighlightRule.decode(line = it) })
    val highlightRules: StateFlow<List<HighlightRule>> = _highlightRules.asStateFlow()

    /** A null [colorArgb] goes back to the theme color. */
    fun setLevelColor(
        level: LogLevel,
        colorArgb: Long?,
    ) {
        val updated = _levelColors.value.toMutableMap()
        if (colorArgb == null) updated.remove(level) else updated[level] = colorArgb
        // Replaced, not changed in place, so the settings store sees the change.
        settings.state.levelColors = updated.entries.associate { it.key.name to it.value.toString(radix = 16) }.toMutableMap()
        _levelColors.value = updated
    }

    fun addRule(rule: HighlightRule) {
        saveRules(rules = _highlightRules.value.filter { it.pattern != rule.pattern } + rule)
    }

    fun removeRule(rule: HighlightRule) {
        saveRules(rules = _highlightRules.value - rule)
    }

    private fun saveRules(rules: List<HighlightRule>) {
        settings.state.highlightRules = rules.map { it.encode() }.toMutableList()
        _highlightRules.value = rules
    }

    private fun loadLevelColors(): Map<LogLevel, Long> = settings.state.levelColors.mapNotNull { (name, value) ->
        val level = LogLevel.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
        val color = value.toLongOrNull(radix = 16) ?: return@mapNotNull null
        level to color
    }.toMap()

    companion object {
        fun getInstance(): AppearanceStore = service()
    }
}
