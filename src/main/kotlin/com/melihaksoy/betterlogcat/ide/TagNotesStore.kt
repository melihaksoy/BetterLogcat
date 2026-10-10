package com.melihaksoy.betterlogcat.ide

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.melihaksoy.betterlogcat.model.TagGroup
import com.melihaksoy.betterlogcat.model.TagGroups
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The user's own tag descriptions and tag groups. Same for every project, since tags come from
 * Android and libraries, not from one project. One store, so every pane updates at once.
 */
@Service(Service.Level.APP)
class TagNotesStore {

    private val settings = BetterLogcatSettings.getInstance()

    private val _notes = MutableStateFlow(value = settings.state.tagNotes.toMap())
    val notes: StateFlow<Map<String, String>> = _notes.asStateFlow()

    private val _groups = MutableStateFlow(value = settings.state.tagGroups.mapNotNull { TagGroup.decode(line = it) })
    val groups: StateFlow<List<TagGroup>> = _groups.asStateFlow()

    /** A blank [note] removes the user's note, so the built-in description (if any) shows again. */
    fun setNote(
        tag: String,
        note: String,
    ) {
        val trimmed = note.trim()
        val updated = settings.state.tagNotes.toMutableMap()
        if (trimmed.isEmpty()) updated.remove(tag) else updated[tag] = trimmed
        // Replaced, not changed in place, so the settings store sees the change.
        settings.state.tagNotes = updated
        _notes.value = updated.toMap()
    }

    fun addToGroup(
        tag: String,
        groupName: String,
    ) = saveGroups(
        groups = TagGroups.withTag(
            groups = _groups.value,
            tag = tag,
            groupName = groupName,
        ),
    )

    fun removeFromGroup(tag: String) = saveGroups(
        groups = TagGroups.withoutTag(
            groups = _groups.value,
            tag = tag,
        ),
    )

    fun renameGroup(
        from: String,
        to: String,
    ) = saveGroups(
        groups = TagGroups.renamed(
            groups = _groups.value,
            from = from,
            to = to,
        ),
    )

    fun deleteGroup(name: String) = saveGroups(
        groups = TagGroups.deleted(
            groups = _groups.value,
            name = name,
        ),
    )

    private fun saveGroups(groups: List<TagGroup>) {
        settings.state.tagGroups = groups.map { it.encode() }.toMutableList()
        _groups.value = groups
    }

    companion object {
        fun getInstance(): TagNotesStore = service()
    }
}
