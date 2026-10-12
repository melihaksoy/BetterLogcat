package com.melihaksoy.betterlogcat.ui.logcat.popup

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.melihaksoy.betterlogcat.format.IssueReport
import com.melihaksoy.betterlogcat.format.LogCopyFormatter
import com.melihaksoy.betterlogcat.ide.AdbCommands
import com.melihaksoy.betterlogcat.ide.BetterLogcatProjectService
import com.melihaksoy.betterlogcat.ide.NativeSymbolizer
import com.melihaksoy.betterlogcat.ide.SymbolizeResult
import com.melihaksoy.betterlogcat.media.MediaFiles
import com.melihaksoy.betterlogcat.model.DeviceState
import com.melihaksoy.betterlogcat.model.LogProblems
import com.melihaksoy.betterlogcat.model.ProblemSession
import com.melihaksoy.betterlogcat.model.SavedProblem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
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
import com.melihaksoy.betterlogcat.model.ProblemKind
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
        showChooser(
            at = at,
            title = BetterLogcatBundle.message("popup.devices.title"),
            items = current.devices.map { DeviceChoice.Pick(device = it) } + DeviceChoice.PairOverWifi,
            selected = current.selectedDevice?.let { DeviceChoice.Pick(device = it) },
            label = { choice ->
                when (choice) {
                    is DeviceChoice.Pick -> deviceLabel(device = choice.device)
                    DeviceChoice.PairOverWifi -> BetterLogcatBundle.message("wifi.menu")
                }
            },
            onChosen = { choice ->
                when (choice) {
                    is DeviceChoice.Pick -> onIntent(LogcatIntent.SelectDevice(serial = choice.device.serial))
                    DeviceChoice.PairOverWifi -> pairOverWifi()
                }
            },
        )
    }

    /** A row of the device picker: a device, or the Wi-Fi pairing action at the end. */
    private sealed interface DeviceChoice {
        data class Pick(val device: Device) : DeviceChoice
        data object PairOverWifi : DeviceChoice
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
        if (entry.problem.let { it != null && it != ProblemKind.WARNING }) {
            group.add(
                action(text = BetterLogcatBundle.message("menu.copyAsIssue")) {
                    copyAsIssue(
                        entry = entry,
                        processName = current.processNames[entry.pid].orEmpty(),
                    )
                },
            )
        }
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
                    onIntent(LogcatIntent.UngroupTags(tags = listOf(tag)))
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
        val name = askGroupName(
            initial = TagGroups.nextName(groups = state().tagGroups) { BetterLogcatBundle.message("tags.group.defaultName", it) },
        ) ?: return
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
        gutter.problem?.let { problem ->
            group.addSeparator()
            val copyKey = when (problem) {
                ProblemKind.CRASH -> "gutter.copyCrash"
                ProblemKind.ANR, ProblemKind.EXCEPTION, ProblemKind.STRICT_MODE -> "gutter.copyException"
                ProblemKind.WARNING -> null
            }
            copyKey?.let { group.add(action(text = BetterLogcatBundle.message(it)) { copyText(text = entry.message) }) }
            if (problem != ProblemKind.WARNING) {
                group.add(
                    action(text = BetterLogcatBundle.message("menu.copyAsIssue")) {
                        copyAsIssue(
                            entry = entry,
                            processName = current.processNames[entry.pid].orEmpty(),
                        )
                    },
                )
            }
            if (LogProblems.isNativeCrash(tag = entry.tag, message = entry.message)) {
                group.add(action(text = BetterLogcatBundle.message("native.symbolize")) { onIntent(LogcatIntent.SymbolizeNativeCrash(id = entry.id)) })
            }
            group.add(
                action(text = BetterLogcatBundle.message("gutter.showProblems")) {
                    if (current.viewOptions.sidePanel != SidePanel.PROBLEMS) onIntent(LogcatIntent.ToggleSidePanel(panel = SidePanel.PROBLEMS))
                },
            )
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

    /** Asks for a base URL that the log hides. */
    private fun addHiddenUrlBase() {
        val base = Messages.showInputDialog(
            project,
            BetterLogcatBundle.message("hiddenUrls.prompt"),
            BetterLogcatBundle.message("hiddenUrls.dialogTitle"),
            null,
        ) ?: return
        onIntent(LogcatIntent.AddHiddenUrlBase(base = base))
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
            onAddUrlBase = ::addHiddenUrlBase,
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

    override fun showProblemMenu(
        session: ProblemSession,
        problem: SavedProblem,
        at: Point,
    ) {
        val group = DefaultActionGroup()
        group.add(
            action(text = BetterLogcatBundle.message("problems.menu.goTo")) {
                onIntent(
                    LogcatIntent.JumpToProblem(
                        sessionId = session.id,
                        key = problem.key,
                        back = 0,
                    ),
                )
            },
        )
        group.add(action(text = BetterLogcatBundle.message("menu.openInEditor")) { openInEditor(entry = problem.toLogEntry()) })
        group.add(action(text = BetterLogcatBundle.message("problems.menu.copy")) { copyText(text = problem.sample) })
        if (problem.kind != ProblemKind.WARNING) {
            group.add(
                action(text = BetterLogcatBundle.message("menu.copyAsIssue")) {
                    copyAsIssue(
                        entry = problem.toLogEntry(),
                        processName = problem.processName,
                    )
                },
            )
        }
        group.addSeparator()
        group.add(action(text = BetterLogcatBundle.message("menu.showOnlyTag", problem.tag)) { onIntent(LogcatIntent.ShowOnlyTag(tag = problem.tag)) })
        group.add(action(text = BetterLogcatBundle.message("menu.hideTag", problem.tag)) { onIntent(LogcatIntent.HideTag(tag = problem.tag)) })
        group.add(action(text = BetterLogcatBundle.message("problems.menu.ignore")) { onIntent(LogcatIntent.IgnoreProblem(key = problem.key)) })
        group.addSeparator()
        group.add(
            action(text = BetterLogcatBundle.message("problems.menu.delete")) {
                onIntent(
                    LogcatIntent.DeleteProblem(
                        sessionId = session.id,
                        key = problem.key,
                    ),
                )
            },
        )
        showMenu(
            at = at,
            group = group,
        )
    }

    override fun showProblemSessionMenu(
        session: ProblemSession,
        at: Point,
    ) {
        val group = DefaultActionGroup()
        group.add(
            action(text = BetterLogcatBundle.message("problems.menu.rename")) {
                val title = Messages.showInputDialog(
                    project,
                    BetterLogcatBundle.message("problems.rename.prompt"),
                    BetterLogcatBundle.message("problems.rename.title"),
                    null,
                    session.title,
                    null,
                )
                if (!title.isNullOrBlank()) {
                    onIntent(
                        LogcatIntent.RenameProblemSession(
                            id = session.id,
                            title = title,
                        ),
                    )
                }
            },
        )
        group.add(action(text = BetterLogcatBundle.message("problems.menu.copySummary")) { copyText(text = sessionSummary(session = session)) })
        group.addSeparator()
        group.add(action(text = BetterLogcatBundle.message("problems.menu.deleteSession")) { onIntent(LogcatIntent.DeleteProblemSession(id = session.id)) })
        group.add(
            action(text = BetterLogcatBundle.message("problems.menu.deleteBefore")) {
                onIntent(LogcatIntent.DeleteProblemHistoryBefore(epochMillis = session.startedEpochMillis))
            },
        )
        showMenu(
            at = at,
            group = group,
        )
    }

    override fun showProblemsPanelMenu(at: Point) {
        val group = DefaultActionGroup()
        val now = System.currentTimeMillis()
        listOf(1, 7, 30).forEach { days ->
            group.add(
                action(text = BetterLogcatBundle.message("problems.menu.deleteOlder", days)) {
                    onIntent(LogcatIntent.DeleteProblemHistoryBefore(epochMillis = now - TimeUnit.DAYS.toMillis(days.toLong())))
                },
            )
        }
        group.add(action(text = BetterLogcatBundle.message("problems.menu.deleteAll")) { onIntent(LogcatIntent.DeleteProblemHistoryBefore(epochMillis = Long.MAX_VALUE)) })
        val ignored = state().problems.ignoredCount
        if (ignored > 0) {
            group.addSeparator()
            group.add(action(text = BetterLogcatBundle.message("problems.menu.resetIgnored", ignored)) { onIntent(LogcatIntent.ResetIgnoredProblems) })
        }
        group.addSeparator()
        group.add(action(text = BetterLogcatBundle.message("problems.menu.settings")) { openSettings() })
        showMenu(
            at = at,
            group = group,
        )
    }

    override fun showExportMenu(at: Point) {
        val group = DefaultActionGroup()
        group.add(action(text = BetterLogcatBundle.message("export.menu.export")) { onIntent(LogcatIntent.Export) })
        group.add(action(text = BetterLogcatBundle.message("export.menu.open")) { chooseLogFile { onIntent(LogcatIntent.OpenLogFile(file = it)) } })
        group.add(action(text = BetterLogcatBundle.message("export.menu.compare")) { chooseLogFile { compareWithFile(file = it) } })
        if (state().openedFileName.isNotEmpty()) {
            group.addSeparator()
            group.add(action(text = BetterLogcatBundle.message("file.backToLive")) { onIntent(LogcatIntent.CloseLogFile) })
        }
        showMenu(
            at = at,
            group = group,
        )
    }

    override fun showNetworkCallMenu(
        call: NetworkCall,
        at: Point,
    ) {
        val group = DefaultActionGroup()
        group.add(action(text = BetterLogcatBundle.message("network.copyPath")) { copyText(text = call.path) })
        group.add(action(text = BetterLogcatBundle.message("network.copyUrl")) { copyText(text = call.url) })
        val request = state().entries.firstOrNull { it.id == call.requestEntryId }
        request?.let { CurlBuilder.fromMessage(message = it.message) }?.let { curl ->
            group.add(action(text = BetterLogcatBundle.message("menu.copyCurl")) { copyText(text = curl) })
        }
        showMenu(
            at = at,
            group = group,
        )
    }

    override fun noticeCrash(
        entryId: Long,
        kind: ProblemKind,
        title: String,
    ) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return
        if (toolWindow.isVisible) return
        val key = if (kind == ProblemKind.ANR) "notification.anr" else "notification.crash"
        NotificationGroupManager.getInstance()
            .getNotificationGroup(MediaFiles.NOTIFICATION_GROUP)
            .createNotification(BetterLogcatBundle.message(key, title), NotificationType.ERROR)
            .addAction(
                NotificationAction.createSimpleExpiring(BetterLogcatBundle.message("notification.show")) {
                    toolWindow.activate { onIntent(LogcatIntent.JumpToEntry(id = entryId)) }
                },
            )
            .notify(project)
    }

    override fun symbolizeNativeCrash(
        crash: LogEntry,
        backtrace: List<String>,
    ) {
        val service = project.service<BetterLogcatProjectService>()
        service.scope.launch {
            val result = withContext(context = Dispatchers.IO) { NativeSymbolizer(project = project).symbolize(backtrace = backtrace) }
            withContext(context = Dispatchers.EDT) {
                when (result) {
                    is SymbolizeResult.Done -> openText(
                        name = "native-crash-${crash.pid}.txt",
                        text = crash.message + "\n\n" + result.text,
                    )
                    SymbolizeResult.NoBacktrace -> MediaFiles.notifyError(project = project, message = BetterLogcatBundle.message("native.noBacktrace"))
                    SymbolizeResult.NoSymbolizer -> MediaFiles.notifyError(project = project, message = BetterLogcatBundle.message("native.noSymbolizer"))
                    is SymbolizeResult.NoLibraries -> MediaFiles.notifyError(
                        project = project,
                        message = BetterLogcatBundle.message("native.noLibraries", result.names.joinToString(separator = ", ")),
                    )
                }
            }
        }
    }

    /** Copies [entry] as Markdown for an issue, with the app version read from the device. */
    private fun copyAsIssue(
        entry: LogEntry,
        processName: String,
    ) {
        val current = state()
        val device = current.selectedDevice
        val packageName = processName.substringBefore(delimiter = ':')
        val service = project.service<BetterLogcatProjectService>()
        service.scope.launch {
            val version = if (device?.state == DeviceState.ONLINE && packageName.isNotEmpty()) {
                AdbCommands(session = service.adbSession).appVersion(
                    serial = device.serial,
                    packageName = packageName,
                )
            } else {
                null
            }
            val text = IssueReport.markdown(
                entry = entry,
                packageName = packageName,
                version = version,
                device = device,
            )
            withContext(context = Dispatchers.EDT) {
                copyText(text = text)
                WindowManager.getInstance().getStatusBar(project)?.info = BetterLogcatBundle.message("status.copiedIssue")
            }
        }
    }

    private fun sessionSummary(session: ProblemSession): String = buildString {
        append(session.title).append('\n')
        session.problems.forEach { problem ->
            append("- ").append(problem.title)
            if (problem.detail.isNotEmpty()) append(": ").append(problem.detail)
            append(" (").append(problem.tag).append(", ×").append(problem.count).append(")\n")
        }
    }

    private fun chooseLogFile(onChosen: (File) -> Unit) {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("txt")
            .withTitle(BetterLogcatBundle.message("file.choose.title"))
        FileChooser.chooseFile(descriptor, project, null) { chosen -> onChosen(File(chosen.path)) }
    }

    /** The shown log next to a saved log file, in the IDE's diff viewer. */
    private fun compareWithFile(file: File) {
        val current = state()
        val text = LogCopyFormatter.format(
            entries = current.entries,
            mode = CopyMode.FULL,
            processNames = current.processNames,
        )
        val factory = DiffContentFactory.getInstance()
        val request = SimpleDiffRequest(
            BetterLogcatBundle.message("compare.fileTitle", file.name),
            factory.create(project, text, PlainTextFileType.INSTANCE),
            factory.create(project, runCatching { file.readText() }.getOrDefault(defaultValue = ""), PlainTextFileType.INSTANCE),
            BetterLogcatBundle.message("compare.current"),
            file.name,
        )
        DiffManager.getInstance().showDiff(project, request)
    }

    private fun openText(
        name: String,
        text: String,
    ) {
        val file = LightVirtualFile(name, PlainTextFileType.INSTANCE, text)
        file.isWritable = false
        FileEditorManager.getInstance(project).openFile(file, true)
    }

    private fun pairOverWifi() {
        val dialog = WifiPairingDialog(project = project)
        if (!dialog.showAndGet()) return
        val service = project.service<BetterLogcatProjectService>()
        val commands = AdbCommands(session = service.adbSession)
        service.scope.launch {
            val answers = buildList {
                if (dialog.pairAddress.isNotEmpty()) {
                    add(
                        runCatching {
                            commands.pair(
                                address = dialog.pairAddress,
                                code = dialog.pairCode,
                            )
                        }.getOrElse { it.message.orEmpty() },
                    )
                }
                if (dialog.connectAddress.isNotEmpty()) {
                    add(runCatching { commands.connect(address = dialog.connectAddress) }.getOrElse { it.message.orEmpty() })
                }
            }
            val text = answers.joinToString(separator = "\n").ifBlank { BetterLogcatBundle.message("wifi.noAnswer") }
            val isFailed = answers.any { answer -> FAILURE_WORDS.any { answer.contains(other = it, ignoreCase = true) } }
            NotificationGroupManager.getInstance()
                .getNotificationGroup(MediaFiles.NOTIFICATION_GROUP)
                .createNotification(
                    BetterLogcatBundle.message("wifi.title"),
                    text,
                    if (isFailed) NotificationType.ERROR else NotificationType.INFORMATION,
                )
                .notify(project)
        }
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
        showLater { builder.createPopup().show(RelativePoint(owner(), at)) }
    }

    /**
     * Shows a popup after the click that asked for it is done. Shown during the click, the Compose panel
     * takes the focus back when it finishes the click, and the popup's search field gets no typing.
     */
    private fun showLater(show: () -> Unit) {
        ApplicationManager.getApplication().invokeLater({ if (owner().isShowing) show() }, project.disposed)
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
            .let { popup -> showLater { popup.show(RelativePoint(owner(), at)) } }
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
        private const val TOOL_WINDOW_ID = "Better Logcat"
        private val FAILURE_WORDS = listOf("failed", "error", "unable", "cannot", "timed out")

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
