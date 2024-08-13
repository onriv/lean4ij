package lean4ij.project.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project

class LeanFileEditorManagerListener(private val project: Project) : FileEditorManagerListener {

    private val leanFileCaretListener : LeanFileCaretListener = project.service()

    override fun selectionChanged(event: FileEditorManagerEvent) {
        event.manager.selectedTextEditor?.let {
            leanFileCaretListener.update(it)
        }
    }
}