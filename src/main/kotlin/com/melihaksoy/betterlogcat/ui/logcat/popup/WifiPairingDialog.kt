package com.melihaksoy.betterlogcat.ui.logcat.popup

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import javax.swing.JComponent

/**
 * Pair a device over Wi-Fi with a pairing code (Developer options › Wireless debugging › Pair device
 * with pairing code), then connect to it. Either step is optional: leave the pairing fields empty
 * for a device that is already paired.
 */
class WifiPairingDialog(project: Project) : DialogWrapper(project, true) {

    var pairAddress: String = ""
        private set
    var pairCode: String = ""
        private set
    var connectAddress: String = ""
        private set

    private lateinit var form: DialogPanel

    init {
        title = BetterLogcatBundle.message("wifi.title")
        setOKButtonText(BetterLogcatBundle.message("wifi.ok"))
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row { comment(BetterLogcatBundle.message("wifi.hint")) }
        group(BetterLogcatBundle.message("wifi.pair")) {
            row(BetterLogcatBundle.message("wifi.pairAddress")) {
                textField()
                    .align(AlignX.FILL)
                    .bindText(
                        getter = { pairAddress },
                        setter = { pairAddress = it.trim() },
                    )
                    .comment(BetterLogcatBundle.message("wifi.pairAddress.comment"))
            }
            row(BetterLogcatBundle.message("wifi.code")) {
                textField()
                    .bindText(
                        getter = { pairCode },
                        setter = { pairCode = it.trim() },
                    )
            }
        }
        group(BetterLogcatBundle.message("wifi.connect")) {
            row(BetterLogcatBundle.message("wifi.connectAddress")) {
                textField()
                    .align(AlignX.FILL)
                    .bindText(
                        getter = { connectAddress },
                        setter = { connectAddress = it.trim() },
                    )
                    .comment(BetterLogcatBundle.message("wifi.connectAddress.comment"))
            }
        }
    }.also { form = it }

    override fun doValidate(): ValidationInfo? {
        form.apply()
        return when {
            pairAddress.isEmpty() && connectAddress.isEmpty() -> ValidationInfo(BetterLogcatBundle.message("wifi.error.empty"))
            pairAddress.isNotEmpty() && pairCode.isEmpty() -> ValidationInfo(BetterLogcatBundle.message("wifi.error.code"))
            else -> null
        }
    }
}
