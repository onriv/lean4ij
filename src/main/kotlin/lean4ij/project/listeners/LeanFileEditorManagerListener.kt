package lean4ij.project.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Listens for editor tab changes and file open events to update the caret listener registration.
 *
 * When the user switches between editor tabs or opens a new file, this listener ensures
 * that [LeanFileCaretListener] is attached to the newly active editor. This is necessary
 * because IntelliJ creates new editor instances for each tab, and the caret listener needs
 * to follow the active editor.
 *
 * @see LeanFileCaretListener.update for the listener registration logic
 */
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