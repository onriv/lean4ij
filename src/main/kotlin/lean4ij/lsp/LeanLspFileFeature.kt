package lean4ij.lsp

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.redhat.devtools.lsp4ij.client.features.LSPFileFeature

class LeanLspFileFeature : LSPFileFeature() {
    override fun isEnabled(file: VirtualFile): Boolean =
        FileEditorManager.getInstance(project).selectedTextEditor?.let {
            it.virtualFile == file
        }?: false
}
