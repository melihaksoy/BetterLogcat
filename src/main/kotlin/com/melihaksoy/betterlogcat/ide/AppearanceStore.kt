package com.melihaksoy.betterlogcat.ide

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.melihaksoy.betterlogcat.model.HighlightPreset
import com.melihaksoy.betterlogcat.model.HighlightRole
import com.melihaksoy.betterlogcat.model.HighlightRule
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.PresetHighlights
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User colors (level color overrides, keyword highlights, library presets) and hidden base URLs.
 * One store, so every pane updates at once.
 */
@Service(Service.Level.APP)
class AppearanceStore {

    private val settings = BetterLogcatSettings.getInstance()

    private val _levelColors = MutableStateFlow(value = loadLevelColors())
    val levelColors: StateFlow<Map<LogLevel, Long>> = _levelColors.asStateFlow()

    private val _highlightRules = MutableStateFlow(value = settings.state.highlightRules.mapNotNull { HighlightRule.decode(line = it) })

    /** The user's own keywords. Preset rules are not in here. */
    val highlightRules: StateFlow<List<HighlightRule>> = _highlightRules.asStateFlow()

    private val _presets = MutableStateFlow(value = loadPresets())
    val presets: StateFlow<PresetHighlights> = _presets.asStateFlow()

    private val _hiddenUrlBases = MutableStateFlow(value = settings.state.hiddenUrlBases.toList())

    /** Base URLs the log hides, so a link shows only its path. */
    val hiddenUrlBases: StateFlow<List<String>> = _hiddenUrlBases.asStateFlow()

    init {
        val (ownRules, presets) = PresetHighlights.migrateLegacy(
            rules = _highlightRules.value,
            current = _presets.value,
        )
        if (ownRules != _highlightRules.value) {
            saveRules(rules = ownRules)
            savePresets(presets = presets)
        }
    }

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

    fun setPresetEnabled(
        preset: HighlightPreset,
        isEnabled: Boolean,
    ) {
        val current = _presets.value
        savePresets(presets = current.copy(enabledIds = if (isEnabled) current.enabledIds + preset.id else current.enabledIds - preset.id))
    }

    fun setPresetRoleEnabled(
        preset: HighlightPreset,
        role: HighlightRole,
        isEnabled: Boolean,
    ) {
        val current = _presets.value
        val key = PresetHighlights.roleKey(
            preset = preset,
            role = role,
        )
        savePresets(presets = current.copy(disabledRoles = if (isEnabled) current.disabledRoles - key else current.disabledRoles + key))
    }

    /** A null [colorArgb] goes back to the role's default. Applies to every preset with [role]. */
    fun setRoleColor(
        role: HighlightRole,
        colorArgb: Long?,
    ) {
        val current = _presets.value
        savePresets(presets = current.copy(roleColors = if (colorArgb == null) current.roleColors - role else current.roleColors + (role to colorArgb)))
    }

    /** Adds [base] without a trailing slash. A blank or known base changes nothing. */
    fun addHiddenUrlBase(base: String) {
        val trimmed = base.trim().trimEnd('/')
        if (trimmed.isEmpty() || _hiddenUrlBases.value.any { it.equals(other = trimmed, ignoreCase = true) }) return
        saveHiddenUrlBases(bases = _hiddenUrlBases.value + trimmed)
    }

    fun removeHiddenUrlBase(base: String) {
        saveHiddenUrlBases(bases = _hiddenUrlBases.value - base)
    }

    private fun saveHiddenUrlBases(bases: List<String>) {
        // Replaced, not changed in place, so the settings store sees the change.
        settings.state.hiddenUrlBases = bases.toMutableList()
        _hiddenUrlBases.value = bases
    }

    private fun saveRules(rules: List<HighlightRule>) {
        settings.state.highlightRules = rules.map { it.encode() }.toMutableList()
        _highlightRules.value = rules
    }

    private fun savePresets(presets: PresetHighlights) {
        // Replaced, not changed in place, so the settings store sees the change.
        settings.state.enabledPresets = presets.enabledIds.toMutableList()
        settings.state.presetRoleColors = presets.roleColors.entries.associate { it.key.id to it.value.toString(radix = 16) }.toMutableMap()
        settings.state.disabledPresetRoles = presets.disabledRoles.toMutableList()
        _presets.value = presets
    }

    private fun loadLevelColors(): Map<LogLevel, Long> = settings.state.levelColors.mapNotNull { (name, value) ->
        val level = LogLevel.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
        val color = value.toLongOrNull(radix = 16) ?: return@mapNotNull null
        level to color
    }.toMap()

    private fun loadPresets(): PresetHighlights = PresetHighlights(
        enabledIds = settings.state.enabledPresets.toSet(),
        roleColors = settings.state.presetRoleColors.mapNotNull { (id, value) ->
            val role = HighlightRole.fromId(id = id) ?: return@mapNotNull null
            val color = value.toLongOrNull(radix = 16) ?: return@mapNotNull null
            role to color
        }.toMap(),
        disabledRoles = settings.state.disabledPresetRoles.toSet(),
    )

    companion object {
        fun getInstance(): AppearanceStore = service()
    }
}
