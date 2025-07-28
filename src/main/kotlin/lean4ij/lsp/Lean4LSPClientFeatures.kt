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
        // The Lean LSP expects the client to announce insertReplaceSupport, otherwise it never sends textEdit data.
        // It still only provides it in some cases (see below), so we still need to provide our own to avoid
        // a bug where it would think that the completion starts at pos 0 (and clear everything before the caret).
        // However, if the LSP provides its own textEdit, we might as well use what it suggests.
        // An LSP test that expects textEdit (though note that most others don't expect it):
        // https://github.com/leanprover/lean4/blob/9dc4dbebe136522a6226a0a4ff6552526cbce3bb/tests/lean/interactive/completionOption.lean.expected.out#L38
        // The LSP code that checks for insertReplaceSupport:
        // https://github.com/leanprover/lean4/blob/dedd9275ec162e181bbbd11ce65ba3bdfbf38e02/src/Lean/Server/Completion/CompletionCollectors.lean#L528
        initializeParams.capabilities.textDocument.completion.completionItem.insertReplaceSupport = true
        super.initializeParams(initializeParams)
    }
}
