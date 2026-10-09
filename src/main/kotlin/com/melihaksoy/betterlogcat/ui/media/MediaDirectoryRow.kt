package com.melihaksoy.betterlogcat.ui.media

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.dsl.builder.Panel
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.media.MediaFiles
import java.io.File

/** "Saving to: <folder>  Configure" row, shared by the screenshot and recording dialogs. */
internal fun Panel.mediaDirectoryRow(project: Project) {
    row(BetterLogcatBundle.message("media.savingTo")) {
        val folder = label(MediaFiles.directory().absolutePath)
        link(BetterLogcatBundle.message("media.configure")) {
            val current = LocalFileSystem.getInstance().findFileByIoFile(MediaFiles.directory())
            FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, current) { chosen ->
                MediaFiles.setDirectory(directory = File(chosen.path))
                folder.component.text = chosen.path
            }
        }
    }
}
