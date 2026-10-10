package com.melihaksoy.betterlogcat.model

import androidx.compose.runtime.Immutable

/**
 * The user's preset choices: which presets are on, role colors that replace the defaults, and roles
 * turned off inside one preset. Role colors are shared by all presets; turned-off roles are not.
 */
@Immutable
data class PresetHighlights(
    val enabledIds: Set<String> = emptySet(),
    val roleColors: Map<HighlightRole, Long> = emptyMap(),
    /** [roleKey] values. */
    val disabledRoles: Set<String> = emptySet(),
) {
    fun isEnabled(preset: HighlightPreset): Boolean = preset.id in enabledIds

    fun isRoleEnabled(
        preset: HighlightPreset,
        role: HighlightRole,
    ): Boolean = roleKey(
        preset = preset,
        role = role,
    ) !in disabledRoles

    fun color(role: HighlightRole): Long = roleColors[role] ?: role.defaultArgb

    /** The rules that paint, in preset order. */
    fun rules(): List<HighlightRule> = HighlightPreset.all
        .filter { isEnabled(preset = it) }
        .flatMap { preset ->
            preset.rules
                .filter { rule ->
                    isRoleEnabled(
                        preset = preset,
                        role = rule.role,
                    )
                }
                .map { rule ->
                    HighlightRule(
                        pattern = rule.pattern,
                        colorArgb = color(role = rule.role),
                        isRegex = true,
                    )
                }
        }

    companion object {
        fun roleKey(
            preset: HighlightPreset,
            role: HighlightRole,
        ): String = "${preset.id}/${role.id}"

        /**
         * Before presets were linked, adding one copied its rules into the user's own keywords. Takes
         * every preset whose patterns are all there back out of [rules], turns the preset on and keeps
         * changed colors as role colors. A preset with a pattern missing stays as plain keywords.
         */
        fun migrateLegacy(
            rules: List<HighlightRule>,
            current: PresetHighlights,
        ): Pair<List<HighlightRule>, PresetHighlights> {
            var ownRules = rules
            var presets = current
            HighlightPreset.all.forEach { preset ->
                val byPattern = ownRules.associateBy { it.pattern }
                val legacy = preset.rules.map { byPattern[it.pattern] ?: return@forEach }
                val changedColors = preset.rules.zip(legacy)
                    .filter { (presetRule, legacyRule) -> legacyRule.colorArgb != presetRule.role.defaultArgb }
                    .associate { (presetRule, legacyRule) -> presetRule.role to legacyRule.colorArgb }
                ownRules = ownRules - legacy.toSet()
                presets = presets.copy(
                    enabledIds = presets.enabledIds + preset.id,
                    roleColors = changedColors + presets.roleColors,
                )
            }
            return ownRules to presets
        }
    }
}
