package lean4ij.project.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class LeanFileEditorManagerListener(private val project: Project) : FileEditorManagerListener {

    private val leanFileCaretListener : LeanFileCaretListener = project.service()

    override fun selectionChanged(event: FileEditorManagerEvent) {
        event.manager.selectedTextEditor?.let {
            leanFileCaretListener.update(it)
        }
    }

    /**
     * For fixing issue
     * https://github.com/onriv/lean4ij/issues/77
     */
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        source.selectedTextEditor?.let {
            leanFileCaretListener.update(it)
        }
    }
}