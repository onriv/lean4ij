package lean4ij.project.listeners

import com.google.common.base.MoreObjects
import com.google.common.base.MoreObjects.firstNonNull
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileOpenedSyncListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.openapi.vfs.VirtualFile
import lean4ij.language.DiagInlayManager
import lean4ij.util.LeanUtil

class LeanFileOpenedListener: FileOpenedSyncListener {
    override fun fileOpenedSync(
        source: FileEditorManager,
        file: VirtualFile,
        editorsWithProviders: List<FileEditorWithProvider>
    ) {
        super.fileOpenedSync(source, file, editorsWithProviders)
        if (!LeanUtil.isLeanFile(file)) return

        // install diag hint listener to all opened files
        for (editorWrapper in editorsWithProviders) {
            val editor = editorWrapper.fileEditor
            if (!LeanUtil.isLeanFile(editor.file)) {
                continue
            }

            if (editor is TextEditor) {
                DiagInlayManager.register(editor)
            }
        }
    }
}