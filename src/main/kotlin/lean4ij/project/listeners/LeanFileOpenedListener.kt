package lean4ij.project.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileOpenedSyncListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.openapi.vfs.VirtualFile
import lean4ij.language.DiagInlayManager
import lean4ij.project.ToolchainService
import lean4ij.util.LeanUtil
import lean4ij.util.notifyErr

class LeanFileOpenedListener: FileOpenedSyncListener {
    override fun fileOpenedSync(
        source: FileEditorManager,
        file: VirtualFile,
        editorsWithProviders: List<FileEditorWithProvider>
    ) {
        super.fileOpenedSync(source, file, editorsWithProviders)
        if (!LeanUtil.isLeanFile(file)) return

        val toolchainService = source.project.service<ToolchainService>();
        if (toolchainService.toolchainNotFound()) {
            val content =
                "Unable to locate lean toolchain. Please verify you opened a lean project. Expected toolchain location: ${toolchainService.expectedToolchainPath()}. "
            source.project.notifyErr(content)
            return;
        }

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