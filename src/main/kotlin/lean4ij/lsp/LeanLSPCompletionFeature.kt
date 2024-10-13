package lean4ij.lsp

import com.intellij.openapi.components.service
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.client.features.LSPCompletionFeature
import lean4ij.Lean4Settings

/**
 * Add an impl for disable lsp completion for lean
 * for sometimes it's slow...
 */
class LeanLSPCompletionFeature : LSPCompletionFeature() {
    private val lean4Settings = service<Lean4Settings>()

    override fun isEnabled(file: PsiFile): Boolean {
        return lean4Settings.enableLspCompletion
    }
}