package lean4ij.lsp

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.client.features.LSPDiagnosticFeature
import com.redhat.devtools.lsp4ij.client.features.LSPWorkspaceSymbolFeature
import lean4ij.project.LeanProjectService
import lean4ij.setting.Lean4Settings
import org.eclipse.lsp4j.InitializeParams

/**
 * per project with the getProject method in the base class
 */
class Lean4LSPClientFeatures : LSPClientFeatures() {
    private val lean4Settings = service<Lean4Settings>()

    init {
        completionFeature = LeanLSPCompletionFeature()
        diagnosticFeature = object : LSPDiagnosticFeature() {
            override fun isEnabled(file: PsiFile): Boolean {
                return true
            }
        }
        // for currently we need some performance tuning
        // for seemingly no cancelRequests handled in the language server end
        // we do the workspace symbol feature manually
        workspaceSymbolFeature = object : LSPWorkspaceSymbolFeature() {
            override fun isEnabled(): Boolean {
                return false
            }
        }
    }

    override fun isEnabled(file: VirtualFile): Boolean {
        if (lean4Settings.fileProgressTriggeringStrategy == "AllOpenedEditor") {
            return true
        }
        val selectedFile = FileEditorManager.getInstance(project)
            .selectedTextEditor?.virtualFile
        return selectedFile == file
    }

    override fun initializeParams(initializeParams: InitializeParams) {
        // Setting this to true should make the LSP start filling out textEdit for completion items
        // However, this currently doesn't seem to work, but it should probably stay here anyway
        // See the test runner for Lean's LSP, which also explicitly sets this to true
        // https://github.com/leanprover/lean4/blob/64219ac91e2930d3950637317c7dc4f7b45568d1/src/Lean/Server/Test/Runner.lean#L94
        initializeParams.capabilities.textDocument.completion.completionItem.insertReplaceSupport = true
        super.initializeParams(initializeParams)
    }
}
