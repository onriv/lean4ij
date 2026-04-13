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

/**
 * Handles synchronous file-open events for Lean files.
 *
 * When a Lean file is opened in the editor, this listener:
 * 1. Verifies the Lean toolchain is available. If not (e.g., the user opened a `.lean` file
 *    outside a Lean project), shows an error notification.
 * 2. Installs [DiagInlayManager] on each opened text editor to display inline diagnostic
 *    hints (e.g., `#check` results shown as inlay text after the line).
 *
 * This uses [FileOpenedSyncListener] (synchronous) rather than the async variant because
 * the diagnostic inlay manager needs to be registered before the editor is fully displayed.
 */
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