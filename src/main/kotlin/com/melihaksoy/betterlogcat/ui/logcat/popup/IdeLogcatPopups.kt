package com.melihaksoy.betterlogcat.ui.logcat.popup

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import java.awt.datatransfer.StringSelection
import com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel
import com.melihaksoy.betterlogcat.ui.logcat.component.GutterUiModel
import com.melihaksoy.betterlogcat.network.NetworkCall
import com.melihaksoy.betterlogcat.format.MessageDocument
import com.melihaksoy.betterlogcat.format.LogTimeFormatter
import com.melihaksoy.betterlogcat.network.CurlBuilder
import com.intellij.testFramework.LightVirtualFile
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.DiffManager
import com.intellij.diff.DiffContentFactory
import com.melihaksoy.betterlogcat.filter.PackageFilter
import com.melihaksoy.betterlogcat.format.CopyMode
import com.melihaksoy.betterlogcat.model.Device
import com.melihaksoy.betterlogcat.model.JsonContent
import com.melihaksoy.betterlogcat.model.LogColumn
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.TimeFormat
import com.melihaksoy.betterlogcat.settings.BetterLogcatConfigurable
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.LogcatState
import com.melihaksoy.betterlogcat.ui.logcat.component.PackageLabels
import com.melihaksoy.betterlogcat.ui.media.MediaActions
import com.melihaksoy.betterlogcat.ui.viewsettings.ViewSettingsDialog
import kotlinx.coroutines.flow.StateFlow
import java.awt.Point
import javax.swing.JComponent
import com.intellij.openapi.ui.InputValidator
import com.melihaksoy.betterlogcat.model.TagDescriptions
import com.melihaksoy.betterlogcat.model.TagGroups

class IdeLogcatPopups(
    private val project: Project,
    private val owner: () -> JComponent,
    private val state: () -> LogcatState,
    private val stateFlow: StateFlow<LogcatState>,
    private val onIntent: (LogcatIntent) -> Unit,
) : LogcatPopups {

    private var viewSettings: ViewSettingsDialog? = null

    override fun showDevicePicker(at: Point) {
        val current = state()
        if (current.devices.isEmpty()) return
        showChooser(
            at = at,
            title = BetterLogcatBundle.message("popup.devices.title"),
            items = current.devices,
            selected = current.selectedDevice,
            label = ::deviceLabel,
            onChosen = { onIntent(LogcatIntent.SelectDevice(serial = it.serial)) },
        )
    }

    override fun showPackagePicker(at: Point) {
        val current = state()
        val selected = current.filter.packageFilter
        val running = current.packages.map { PackageFilter.Package(name = it) }
        val custom = if (selected is PackageFilter.Package && selected !in running) listOf(selected) else emptyList()
        showChooser(
            at = at,
            title = BetterLogcatBundle.message("popup.packages.title"),
            items = listOf(PackageFilter.ProjectApps, PackageFilter.All) + custom + running,
            selected = selected,
            label = {
                PackageLabels.chooserLabel(
                    filter = it,
                    projectAppIds = current.projectAppIds,
                    aliases = current.packageAliases,
                )
            },
            onChosen = { onIntent(LogcatIntent.SelectPackage(packageFilter = it)) },
        )
    }

    override fun showRowMenu(
        entry: LogEntry,
        at: Point,
    ) {
        val current = state()
        val selected = current.entries.filter { it.id in current.selection.selectedIds }
        val hasJson = selected.any { it.json is JsonContent.Found }
        val processName = current.processNames[entry.pid].orEmpty().substringBefore(':')

        val group = DefaultActionGroup()
        val defaultMode = current.viewOptions.defaultCopyMode
        val otherMode = if (defaultMode == CopyMode.MESSAGE) CopyMode.FULL else CopyMode.MESSAGE
        group.add(
            action(
                text = BetterLogcatBundle.message(if (defaultMode == CopyMode.MESSAGE) "menu.copyMessages" else "menu.copyLines"),
                shortcutFrom = IdeActions.ACTION_COPY,
                onClick = { onIntent(LogcatIntent.Copy(mode = defaultMode)) },
            ),
        )
        group.add(
            action(text = BetterLogcatBundle.message(if (otherMode == CopyMode.MESSAGE) "menu.copyMessages" else "menu.copyLines")) {
                onIntent(LogcatIntent.Copy(mode = otherMode))
            },
        )
        if (hasJson) {
            group.add(action(text = BetterLogcatBundle.message("menu.copyJsonPretty")) { onIntent(LogcatIntent.Copy(mode = CopyMode.JSON_PRETTY)) })
            group.add(action(text = BetterLogcatBundle.message("menu.copyJsonMinified")) { onIntent(LogcatIntent.Copy(mode = CopyMode.JSON_MINIFIED)) })
        }
        group.add(action(text = BetterLogcatBundle.message("menu.openInEditor")) { openInEditor(entry = entry) })
        CurlBuilder.fromMessage(message = entry.message)?.let { curl ->
            group.add(action(text = BetterLogcatBundle.message("menu.copyCurl")) { copyText(text = curl) })
        }
        if (selected.size == 2) {
            group.add(action(text = BetterLogcatBundle.message("menu.compare")) { compare(first = selected[0], second = selected[1]) })
        }
        group.add(
            action(text = BetterLogcatBundle.message(if (entry.id in current.bookmarkedIds) "menu.removeBookmark" else "menu.bookmark")) {
                onIntent(LogcatIntent.ToggleBookmark(id = entry.id))
            },
        )
        group.addSeparator()
        group.add(action(text = BetterLogcatBundle.message("menu.showOnlyTag", entry.tag)) { onIntent(LogcatIntent.ShowOnlyTag(tag = entry.tag)) })
        group.add(action(text = BetterLogcatBundle.message("menu.hideTag", entry.tag)) { onIntent(LogcatIntent.HideTag(tag = entry.tag)) })
        group.add(action(text = BetterLogcatBundle.message("tags.menu.editNoteOf", entry.tag)) { editTagNote(tag = entry.tag) })
        if (processName.isNotEmpty()) {
            group.add(
                action(text = BetterLogcatBundle.message("menu.showOnlyPackage", processName)) {
                    onIntent(LogcatIntent.SelectPackage(packageFilter = PackageFilter.Package(name = processName)))
                },
            )
        }
        if (processName.isNotEmpty()) {
            group.add(action(text = BetterLogcatBundle.message("menu.setAlias", processName)) { editAlias(packageName = processName) })
        }
        if (selected.any { it.isExpandable }) {
            group.addSeparator()
            group.add(action(text = BetterLogcatBundle.message("menu.expand")) { onIntent(LogcatIntent.SetSelectionExpanded(isExpanded = true)) })
            group.add(action(text = BetterLogcatBundle.message("menu.collapse")) { onIntent(LogcatIntent.SetSelectionExpanded(isExpanded = false)) })
        }
        if (selected.any { it.hasStackFrames || it.lines.size > 1 }) {
            group.addSeparator()
            group.add(action(text = BetterLogcatBundle.message("menu.retrace")) { onIntent(LogcatIntent.ChooseMapping) })
        }
        group.addSeparator()
        group.add(
            action(
                text = BetterLogcatBundle.message("menu.selectAll"),
                shortcutFrom = IdeActions.ACTION_SELECT_ALL,
                onClick = { onIntent(LogcatIntent.SelectAll) },
            ),
        )
        group.add(action(text = BetterLogcatBundle.message("menu.clear")) { onIntent(LogcatIntent.Clear) })
        showMenu(
            at = at,
            group = group,
        )
    }

    override fun showTagMenu(
        tag: String,
        at: Point,
    ) {
        val current = state()
        val group = DefaultActionGroup()
        group.add(action(text = BetterLogcatBundle.message("tags.menu.editNote")) { editTagNote(tag = tag) })
        if (!current.tagNotes[tag].isNullOrBlank()) {
            group.add(
                action(text = BetterLogcatBundle.message("tags.menu.resetNote")) {
                    onIntent(
                        LogcatIntent.SetTagNote(
                            tag = tag,
                            note = "",
                        ),
                    )
                },
            )
        }
        group.addSeparator()
        val currentGroup = TagGroups.groupOf(
            groups = current.tagGroups,
            tag = tag,
        )
        val moveTo = DefaultActionGroup(BetterLogcatBundle.message("tags.menu.addToGroup"), true)
        val otherGroups = current.tagGroups.filter { it.name != currentGroup?.name }
        otherGroups.forEach { target ->
            moveTo.add(
                action(text = target.name) {
                    onIntent(
                        LogcatIntent.AddTagToGroup(
                            tag = tag,
                            groupName = target.name,
                        ),
                    )
                },
            )
        }
        if (otherGroups.isNotEmpty()) moveTo.addSeparator()
        moveTo.add(action(text = BetterLogcatBundle.message("tags.menu.newGroup")) { addToNewGroup(tag = tag) })
        group.add(moveTo)
        if (currentGroup != null) {
            group.add(
                action(text = BetterLogcatBundle.message("tags.menu.removeFromGroup", currentGroup.name)) {
                    onIntent(LogcatIntent.RemoveTagFromGroup(tag = tag))
                },
            )
        }
        group.addSeparator()
        group.add(action(text = BetterLogcatBundle.message("menu.showOnlyTag", tag)) { onIntent(LogcatIntent.ShowOnlyTag(tag = tag)) })
        group.add(action(text = BetterLogcatBundle.message("menu.hideTag", tag)) { onIntent(LogcatIntent.HideTag(tag = tag)) })
        showMenu(
            at = at,
            group = group,
        )
    }

    override fun showTagGroupMenu(
        groupName: String,
        at: Point,
    ) {
        val tagGroup = state().tagGroups.firstOrNull { it.name == groupName } ?: return
        val group = DefaultActionGroup()
        if (tagGroup.tags.isNotEmpty()) {
            group.add(
                action(text = BetterLogcatBundle.message("tags.menu.showOnlyGroup")) {
                    onIntent(LogcatIntent.ShowOnlyTags(tags = tagGroup.tags))
                },
            )
            group.addSeparator()
        }
        group.add(action(text = BetterLogcatBundle.message("tags.menu.renameGroup")) { renameGroup(name = groupName) })
        group.add(action(text = BetterLogcatBundle.message("tags.menu.deleteGroup")) { onIntent(LogcatIntent.DeleteTagGroup(name = groupName)) })
        showMenu(
            at = at,
            group = group,
        )
    }

    /** Asks for a description of [tag], filled with the current one. Empty goes back to the built-in one. */
    private fun editTagNote(tag: String) {
        val notes = state().tagNotes
        val current = TagDescriptions.describe(
            tag = tag,
            userNotes = notes,
        ).orEmpty()
        val note = Messages.showInputDialog(
            project,
            BetterLogcatBundle.message("tags.note.prompt", tag),
            BetterLogcatBundle.message("tags.note.title"),
            null,
            current,
            null,
        ) ?: return
        // Saving the built-in text as is would only hide later fixes to it.
        if (notes[tag] == null && note.trim() == TagDescriptions.builtIn[tag]) return
        onIntent(
            LogcatIntent.SetTagNote(
                tag = tag,
                note = note,
            ),
        )
    }

    private fun addToNewGroup(tag: String) {
        val name = askGroupName(initial = "") ?: return
        onIntent(
            LogcatIntent.AddTagToGroup(
                tag = tag,
                groupName = name,
            ),
        )
    }

    private fun renameGroup(name: String) {
        val newName = askGroupName(initial = name) ?: return
        onIntent(
            LogcatIntent.RenameTagGroup(
                from = name,
                to = newName,
            ),
        )
    }

    /** A group name: not blank and not taken by another group. Null when cancelled. */
    private fun askGroupName(initial: String): String? {
        val taken = state().tagGroups.map { it.name }.toSet() - initial
        return Messages.showInputDialog(
            project,
            BetterLogcatBundle.message("tags.group.prompt"),
            BetterLogcatBundle.message("tags.group.title"),
            null,
            initial,
            object : InputValidator {
                override fun checkInput(inputString: String?): Boolean {
                    val name = inputString.orEmpty().trim()
                    return name.isNotEmpty() && name !in taken
                }

                override fun canClose(inputString: String?): Boolean = checkInput(inputString = inputString)
            },
        )?.trim()
    }

    override fun showGutterMenu(
        entry: LogEntry,
        gutter: GutterUiModel,
        at: Point,
    ) {
        val current = state()
        val group = DefaultActionGroup()
        group.add(
            action(text = BetterLogcatBundle.message(if (gutter.isBookmarked) "menu.removeBookmark" else "menu.bookmark")) {
                onIntent(LogcatIntent.ToggleBookmark(id = entry.id))
            },
        )
        gutter.call?.let { call ->
            group.addSeparator()
            if (entry.id == call.requestEntryId && !call.isPending) {
                group.add(
                    action(text = BetterLogcatBundle.message("gutter.goToResponse", call.statusCode, call.durationMillis)) {
                        onIntent(LogcatIntent.JumpToEntry(id = call.responseEntryId))
                    },
                )
            }
            if (entry.id == call.responseEntryId && call.requestEntryId != NetworkCall.NO_ENTRY) {
                group.add(action(text = BetterLogcatBundle.message("gutter.goToRequest")) { onIntent(LogcatIntent.JumpToEntry(id = call.requestEntryId)) })
            }
            val request = current.entries.firstOrNull { it.id == call.requestEntryId }
            request?.let { CurlBuilder.fromMessage(message = it.message) }?.let { curl ->
                group.add(action(text = BetterLogcatBundle.message("menu.copyCurl")) { copyText(text = curl) })
            }
            group.add(
                action(text = BetterLogcatBundle.message("gutter.showNetwork")) {
                    if (current.viewOptions.sidePanel != SidePanel.NETWORK) onIntent(LogcatIntent.ToggleSidePanel(panel = SidePanel.NETWORK))
                },
            )
        }
        if (gutter.isCrash) {
            group.addSeparator()
            group.add(action(text = BetterLogcatBundle.message("gutter.copyCrash")) { copyText(text = entry.message) })
        }
        group.addSeparator()
        group.add(action(text = BetterLogcatBundle.message("menu.openInEditor")) { openInEditor(entry = entry) })
        showMenu(
            at = at,
            group = group,
        )
    }

    /** A read-only editor tab with the message: full editor selection, folding and find for big bodies. */
    override fun openInEditor(entry: LogEntry) {
        val isJson = MessageDocument.isPureJson(entry = entry)
        val fileType = if (isJson) FileTypeManager.getInstance().getFileTypeByExtension("json") else PlainTextFileType.INSTANCE
        val time = LogTimeFormatter.format(
            entry = entry,
            format = TimeFormat.TIME,
            previous = null,
        ).replace(':', '-')
        val name = "${entry.tag.replace(Regex(pattern = "[^A-Za-z0-9._-]+"), "_")}_$time.${if (isJson) "json" else "log"}"
        val file = LightVirtualFile(name, fileType, MessageDocument.text(entry = entry))
        file.isWritable = false
        FileEditorManager.getInstance(project).openFile(file, true)
    }

    private fun compare(
        first: LogEntry,
        second: LogEntry,
    ) {
        val bothJson = MessageDocument.isPureJson(entry = first) && MessageDocument.isPureJson(entry = second)
        val fileType = if (bothJson) FileTypeManager.getInstance().getFileTypeByExtension("json") else PlainTextFileType.INSTANCE
        val factory = DiffContentFactory.getInstance()
        val request = SimpleDiffRequest(
            BetterLogcatBundle.message("compare.title"),
            factory.create(project, MessageDocument.text(entry = first), fileType),
            factory.create(project, MessageDocument.text(entry = second), fileType),
            compareTitle(entry = first),
            compareTitle(entry = second),
        )
        DiffManager.getInstance().showDiff(project, request)
    }

    private fun compareTitle(entry: LogEntry): String = "${LogTimeFormatter.format(entry = entry, format = TimeFormat.TIME, previous = null)}  ${entry.tag}"

    private fun copyText(text: String) {
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    /** Asks for a short display name. An empty answer removes the alias. */
    fun editAlias(packageName: String) {
        val alias = Messages.showInputDialog(
            project,
            BetterLogcatBundle.message("alias.prompt", packageName),
            BetterLogcatBundle.message("alias.title"),
            null,
            state().packageAliases[packageName].orEmpty(),
            null,
        ) ?: return
        onIntent(
            LogcatIntent.SetPackageAlias(
                packageName = packageName,
                alias = alias,
            ),
        )
    }

    /** Asks for a package (running apps offered, any name allowed), then its alias. */
    fun addAlias() {
        val running = state().packages
        val packageName = Messages.showEditableChooseDialog(
            BetterLogcatBundle.message("alias.choosePackage"),
            BetterLogcatBundle.message("alias.title"),
            null,
            running.toTypedArray(),
            running.firstOrNull().orEmpty(),
            null,
        )?.trim().orEmpty()
        if (packageName.isNotEmpty()) editAlias(packageName = packageName)
    }

    override fun openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, BetterLogcatConfigurable::class.java)
    }

    override fun openViewSettings() {
        viewSettings?.takeIf { it.isShowing }?.let {
            it.window.toFront()
            return
        }
        val dialog = ViewSettingsDialog(
            project = project,
            state = stateFlow,
            onIntent = onIntent,
            onOpenIdeSettings = ::openSettings,
            onEditAlias = ::editAlias,
            onAddAlias = ::addAlias,
        )
        viewSettings = dialog
        dialog.show()
    }

    override fun takeScreenshot() {
        val device = state().selectedDevice ?: return
        MediaActions.takeScreenshot(
            project = project,
            device = device,
        )
    }

    override fun recordScreen() {
        val device = state().selectedDevice ?: return
        MediaActions.recordScreen(
            project = project,
            device = device,
        )
    }

    private fun <T> showChooser(
        at: Point,
        title: String,
        items: List<T>,
        selected: T?,
        label: (T) -> String,
        onChosen: (T) -> Unit,
    ) {
        val builder = JBPopupFactory.getInstance()
            .createPopupChooserBuilder(items)
            .setTitle(title)
            .setRenderer(textListCellRenderer { item: T -> label(item) })
            .setNamerForFiltering { label(it) }
            .setFilterAlwaysVisible(true)
            .setItemChosenCallback { onChosen(it) }
        if (selected != null) builder.setSelectedValue(selected, true)
        builder.createPopup().show(RelativePoint(owner(), at))
    }

    /** An IDE list popup, the same kind as the pickers. A Swing JPopupMenu did not show over the Compose panel. */
    private fun showMenu(
        at: Point,
        group: DefaultActionGroup,
    ) {
        JBPopupFactory.getInstance()
            .createActionGroupPopup(
                null,
                group,
                DataManager.getInstance().getDataContext(owner()),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true,
            )
            .show(RelativePoint(owner(), at))
    }

    private fun action(
        text: String,
        shortcutFrom: String = "",
        onClick: () -> Unit,
    ): AnAction {
        val action = DumbAwareAction.create(text) { onClick() }
        if (shortcutFrom.isNotEmpty()) {
            ActionManager.getInstance().getAction(shortcutFrom)?.let { action.copyShortcutFrom(it) }
        }
        return action
    }

    companion object {
        fun deviceLabel(device: Device): String = buildString {
            append(device.name)
            if (device.androidVersion.isNotEmpty()) {
                append("  ·  ")
                append(BetterLogcatBundle.message("device.android", device.androidVersion, device.apiLevel))
            }
        }

        fun timeFormatLabel(format: TimeFormat): String = when (format) {
            TimeFormat.TIME -> BetterLogcatBundle.message("time.time")
            TimeFormat.DATE_TIME -> BetterLogcatBundle.message("time.dateTime")
            TimeFormat.FULL -> BetterLogcatBundle.message("time.full")
            TimeFormat.EPOCH -> BetterLogcatBundle.message("time.epoch")
            TimeFormat.DELTA -> BetterLogcatBundle.message("time.delta")
        }

        fun columnLabel(column: LogColumn): String = when (column) {
            LogColumn.TIME -> BetterLogcatBundle.message("column.time")
            LogColumn.PROCESS -> BetterLogcatBundle.message("column.process")
            LogColumn.PACKAGE -> BetterLogcatBundle.message("column.package")
            LogColumn.TAG -> BetterLogcatBundle.message("column.tag")
            LogColumn.LEVEL -> BetterLogcatBundle.message("column.level")
        }
    }
}
