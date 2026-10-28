package com.melihaksoy.betterlogcat.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.ide.AppearanceStore
import com.melihaksoy.betterlogcat.ide.LastLogStore
import java.awt.Component
import javax.swing.JComponent

class BetterLogcatConfigurable : BoundConfigurable(BetterLogcatBundle.message("settings.displayName")) {

    private val appearance = AppearanceStore.getInstance()

    /** Edited here, saved to [AppearanceStore] on Apply, so open panes update at once. */
    private val urlBases = CollectionListModel(appearance.hiddenUrlBases.value)

    override fun createPanel(): DialogPanel {
        val state = BetterLogcatSettings.getInstance().state
        return panel {
            group(BetterLogcatBundle.message("settings.group.adb")) {
                row(BetterLogcatBundle.message("settings.adbPath")) {
                    textField()
                        .align(AlignX.FILL)
                        .bindText(
                            getter = { state.adbPath.orEmpty() },
                            setter = { state.adbPath = it.trim() },
                        )
                        .comment(BetterLogcatBundle.message("settings.adbPath.comment"))
                }
            }
            group(BetterLogcatBundle.message("settings.group.buffer")) {
                row(BetterLogcatBundle.message("settings.maxEntries")) {
                    intTextField(range = 1_000..2_000_000)
                        .bindIntText(
                            getter = { state.maxEntries },
                            setter = { state.maxEntries = it },
                        )
                }
                row {
                    checkBox(BetterLogcatBundle.message("settings.loadExistingBuffer"))
                        .bindSelected(
                            getter = { state.loadExistingBuffer },
                            setter = { state.loadExistingBuffer = it },
                        )
                }
                row {
                    checkBox(BetterLogcatBundle.message("settings.clearDeviceBuffer"))
                        .bindSelected(
                            getter = { state.clearDeviceBuffer },
                            setter = { state.clearDeviceBuffer = it },
                        )
                }
                row {
                    checkBox(BetterLogcatBundle.message("settings.keepLastLog"))
                        .bindSelected(
                            getter = { state.keepLastLog },
                            setter = { isOn ->
                                state.keepLastLog = isOn
                                if (!isOn) LastLogStore.default().deleteAll()
                            },
                        )
                        .comment(BetterLogcatBundle.message("settings.keepLastLog.comment", LastLogStore.MAX_ENTRIES))
                }
            }
            group(BetterLogcatBundle.message("settings.group.tabs")) {
                row {
                    checkBox(BetterLogcatBundle.message("settings.openTabOnAppSwitch"))
                        .bindSelected(
                            getter = { state.openTabOnAppSwitch },
                            setter = { state.openTabOnAppSwitch = it },
                        )
                        .comment(BetterLogcatBundle.message("settings.openTabOnAppSwitch.comment"))
                }
            }
            group(BetterLogcatBundle.message("settings.group.problems")) {
                row {
                    checkBox(BetterLogcatBundle.message("settings.notifyOnCrash"))
                        .bindSelected(
                            getter = { state.notifyOnCrash },
                            setter = { state.notifyOnCrash = it },
                        )
                }
                row(BetterLogcatBundle.message("settings.problemHistoryDays")) {
                    intTextField(range = 1..365)
                        .bindIntText(
                            getter = { state.problemHistoryDays },
                            setter = { state.problemHistoryDays = it },
                        )
                        .comment(BetterLogcatBundle.message("settings.problemHistoryDays.comment"))
                }
            }
            group(BetterLogcatBundle.message("settings.group.bookmarks")) {
                row {
                    checkBox(BetterLogcatBundle.message("settings.keepBookmarks"))
                        .bindSelected(
                            getter = { state.keepBookmarks },
                            setter = { state.keepBookmarks = it },
                        )
                        .comment(BetterLogcatBundle.message("settings.keepBookmarks.comment"))
                }
            }
            group(BetterLogcatBundle.message("hiddenUrls.title")) {
                row {
                    cell(urlBasesEditor())
                        .align(Align.FILL)
                        .comment(BetterLogcatBundle.message("hiddenUrls.summary"))
                }.resizableRow()
            }
            onApply { appearance.setHiddenUrlBases(bases = urlBases.items) }
            onReset { urlBases.replaceAll(appearance.hiddenUrlBases.value) }
            onIsModified { urlBases.items != appearance.hiddenUrlBases.value }
        }
    }

    private fun urlBasesEditor(): JComponent {
        val list = JBList(urlBases)
        list.emptyText.text = BetterLogcatBundle.message("hiddenUrls.empty")
        return ToolbarDecorator.createDecorator(list)
            .setAddAction {
                askUrlBase(
                    parent = list,
                    title = BetterLogcatBundle.message("hiddenUrls.dialogTitle"),
                    initial = null,
                )?.let { urlBases.add(it) }
            }
            .setEditAction {
                val index = list.selectedIndex
                if (index < 0) return@setEditAction
                askUrlBase(
                    parent = list,
                    title = BetterLogcatBundle.message("hiddenUrls.editDialogTitle"),
                    initial = urlBases.getElementAt(index),
                )?.let { urlBases.setElementAt(it, index) }
            }
            .disableUpDownActions()
            .createPanel()
    }

    /** Null when the user cancels or leaves the field blank. */
    private fun askUrlBase(
        parent: Component,
        title: String,
        initial: String?,
    ): String? = Messages.showInputDialog(
        parent,
        BetterLogcatBundle.message("hiddenUrls.prompt"),
        title,
        null,
        initial,
        null,
    )?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
}
