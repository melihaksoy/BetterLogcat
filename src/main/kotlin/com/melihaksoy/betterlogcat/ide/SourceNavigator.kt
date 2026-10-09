package com.melihaksoy.betterlogcat.ide

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.melihaksoy.betterlogcat.format.StackFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Opens the source file of a stack frame. Prefers project files over library files. */
class SourceNavigator(private val project: Project) {

    /** Returns false when no matching file exists. */
    suspend fun navigate(frame: StackFrame): Boolean {
        val file = smartReadAction(project = project) { find(frame = frame) } ?: return false
        withContext(context = Dispatchers.EDT) {
            OpenFileDescriptor(project, file, (frame.line - 1).coerceAtLeast(minimumValue = 0), 0).navigate(true)
        }
        return true
    }

    private fun find(frame: StackFrame): VirtualFile? {
        val projectScope = GlobalSearchScope.projectScope(project)
        val candidates = FilenameIndex.getVirtualFilesByName(frame.fileName, GlobalSearchScope.allScope(project))
        if (candidates.isEmpty()) return null
        val packagePath = frame.className.substringBeforeLast('.', missingDelimiterValue = "").replace('.', '/')
        val inPackage = candidates.filter { packagePath.isEmpty() || it.path.contains("/$packagePath/") }
        return inPackage.firstOrNull { projectScope.contains(it) }
            ?: inPackage.firstOrNull()
            ?: candidates.firstOrNull { projectScope.contains(it) }
    }
}
