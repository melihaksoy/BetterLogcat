package com.melihaksoy.betterlogcat.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.TimeFormat

@Service(Service.Level.APP)
@State(
    name = "BetterLogcatSettings",
    storages = [Storage("betterLogcat.xml")],
)
class BetterLogcatSettings : SimplePersistentStateComponent<BetterLogcatSettings.SettingsState>(SettingsState()) {

    class SettingsState : BaseState() {
        var adbPath by string(defaultValue = "")
        var maxEntries by property(defaultValue = 100_000)
        var loadExistingBuffer by property(defaultValue = true)
        var clearDeviceBuffer by property(defaultValue = true)

        /** Saves bookmarked lines in the workspace file, so Clear and a restart keep them. */
        var keepBookmarks by property(defaultValue = true)

        /** When the project app starts, the log starts over with it. */
        var clearOnAppLaunch by property(defaultValue = false)

        /** Saves the end of each device's log, so it shows again after an IDE restart. */
        var keepLastLog by property(defaultValue = true)

        /** A crash or ANR shows an IDE notification while the tool window is hidden. */
        var notifyOnCrash by property(defaultValue = true)

        /** Problem sessions older than this are deleted when the project opens. */
        var problemHistoryDays by property(defaultValue = 30)

        var softWrap by property(defaultValue = true)
        var compactRows by property(defaultValue = false)
        var expandByDefault by property(defaultValue = true)
        var prettyJson by property(defaultValue = true)
        var timeFormat by enum(defaultValue = TimeFormat.TIME)
        var sidePanel by string(defaultValue = "NONE")
        var tagsPanelWidth by property(defaultValue = 280)
        var networkPanelWidth by property(defaultValue = 460)
        var bookmarksPanelWidth by property(defaultValue = 360)
        var problemsPanelWidth by property(defaultValue = 380)
        var networkDetailHeight by property(defaultValue = 320)
        var includeThreadId by property(defaultValue = true)
        var showRepeatedTags by property(defaultValue = true)
        var showRepeatedPackages by property(defaultValue = true)
        var colorizeTags by property(defaultValue = true)
        var copyMessageOnly by property(defaultValue = true)
        var expandJsonByDefault by property(defaultValue = false)

        var showTime by property(defaultValue = true)
        var showProcess by property(defaultValue = true)
        var showPackage by property(defaultValue = true)
        var showTag by property(defaultValue = true)
        var showLevel by property(defaultValue = true)

        var timeWidth by property(defaultValue = 112)
        var processWidth by property(defaultValue = 104)
        var packageWidth by property(defaultValue = 180)
        var tagWidth by property(defaultValue = 160)

        /** Filter of the last change, restored in new tabs. Null until the first change. */
        var lastFilter by string(defaultValue = null)
        var favoriteFilters by list<String>()
        var packageAliases by map<String, String>()
        /** The user's own tag descriptions, by tag. They win over the built-in ones. */
        var tagNotes by map<String, String>()
        /** Tag groups the user made in the Tags panel, one `TagGroup.encode` line each. */
        var tagGroups by list<String>()
        var levelColors by map<String, String>()
        var highlightRules by list<String>()

        /** Base URLs the log hides, so a link shows only its path. */
        var hiddenUrlBases by list<String>()
        var enabledPresets by list<String>()

        /** Role id to AARRGGBB hex. */
        var presetRoleColors by map<String, String>()

        /** `presetId/roleId` of roles turned off inside one preset. */
        var disabledPresetRoles by list<String>()
        var filterHistory by list<String>()

        var mediaDirectory by string(defaultValue = "")
        var screenshotScale by property(defaultValue = 100)
        var recordBitRateMbps by property(defaultValue = 4)
        var recordScale by property(defaultValue = 100)
        var recordShowTaps by property(defaultValue = false)
        var recordUseEmulator by property(defaultValue = true)

        /** Comma-separated [LogLevel] names that show. */
        var levels by string(defaultValue = LogLevel.entries.joinToString(separator = ",") { it.name })
        var isRegex by property(defaultValue = false)
        var isMatchCase by property(defaultValue = false)
    }

    companion object {
        fun getInstance(): BetterLogcatSettings = service()
    }
}
