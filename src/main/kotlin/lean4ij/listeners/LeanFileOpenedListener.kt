package lean4ij.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileOpenedSyncListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.openapi.vfs.VirtualFile
import lean4ij.Lean4Settings
import lean4ij.language.DiagInlayManager

class LeanFileOpenedListener: FileOpenedSyncListener {
    override fun fileOpenedSync(
        source: FileEditorManager,
        file: VirtualFile,
        editorsWithProviders: List<FileEditorWithProvider>
    ) {
        super.fileOpenedSync(source, file, editorsWithProviders)

        // install diag hint listener to all opened files
        for (editorWrapper in editorsWithProviders) {
            val editor = editorWrapper.fileEditor
            if (editor is TextEditor) {
                DiagInlayManager.register(editor)
            }
        }
    }
}