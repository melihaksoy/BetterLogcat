package com.melihaksoy.betterlogcat.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.ide.LastLogStore

class BetterLogcatConfigurable : BoundConfigurable(BetterLogcatBundle.message("settings.displayName")) {

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
        }
    }
}
