package com.melihaksoy.betterlogcat.media

import com.intellij.ide.actions.RevealFileAction
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.ide.BrowserUtil
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import java.io.File

/** Save folder and the "saved" notification shared by screenshots and recordings. */
object MediaFiles {

    fun directory(): File {
        val saved = BetterLogcatSettings.getInstance().state.mediaDirectory.orEmpty()
        return if (saved.isNotBlank()) File(saved) else File(System.getProperty("user.home"), "Desktop")
    }

    fun setDirectory(directory: File) {
        BetterLogcatSettings.getInstance().state.mediaDirectory = directory.absolutePath
    }

    fun notifySaved(
        project: Project,
        file: File,
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(
                BetterLogcatBundle.message("media.saved", file.name),
                NotificationType.INFORMATION,
            )
            .addAction(NotificationAction.createSimple(BetterLogcatBundle.message("media.open")) { BrowserUtil.browse(file) })
            .addAction(NotificationAction.createSimple(RevealFileAction.getActionName()) { RevealFileAction.openFile(file) })
            .notify(project)
    }

    fun notifyError(
        project: Project,
        message: String,
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(message, NotificationType.ERROR)
            .notify(project)
    }

    const val NOTIFICATION_GROUP = "Better Logcat"
}
