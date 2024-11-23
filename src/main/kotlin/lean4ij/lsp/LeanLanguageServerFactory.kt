package lean4ij.lsp

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.LanguageServerEnablementSupport
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.client.features.LSPDiagnosticFeature
import com.redhat.devtools.lsp4ij.client.features.LSPWorkspaceSymbolFeature
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import lean4ij.setting.Lean4Settings
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The language server factory as LSP4IJ describe
 */
class LeanLanguageServerFactory : LanguageServerFactory, LanguageServerEnablementSupport {

    private val lean4Settings = service<Lean4Settings>()

    companion object {
        /**
         * Setting this to false rather than true, although it makes the language server does not start as the project
         * or ide opens, but it seems improving performance for avoiding peak cpu flush as the opening
         * TODO add this on readme
         * TODO maybe some settings for it
         * TODO it's back to true, inconsistent with readme
         */
        val isEnable : AtomicBoolean = AtomicBoolean(true)
    }

    /**
     * only if Editor is focus, check  assign logic of isEnable
     * TODO maybe require some refactor
     * check also [lean4ij.project.LeanProjectActivity.setupEditorFocusChangeEventListener]
     * TODO this seems making the lsp server start a little late
     * Beware that this might make debug harder for it become disabled if not focusing on the editor
     * set it to always true if no language server return while debugging
     */
    override fun isEnabled(project: Project): Boolean {
        return lean4Settings.enableLanguageServer && isEnable.get()
    }

    override fun setEnabled(enabled: Boolean, project: Project) {
        // Just ignore the input
    }

    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return LeanLanguageServerProvider(project)
    }

    override fun createLanguageClient(project: Project): LanguageClientImpl {
        return LeanLsp4jClient(project)
    }

    override fun getServerInterface(): Class<out LanguageServer> {
        return InternalLeanLanguageServer::class.java
    }

    override fun createClientFeatures(): LSPClientFeatures {
        // TODO extract this to a standalone class and do some refactor
        return object : LSPClientFeatures() {
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
            // waiting for lsp4ij https://github.com/redhat-developer/lsp4ij/pull/586
            override fun isEnabled(file: VirtualFile): Boolean {
                return FileEditorManager.getInstance(project).selectedTextEditor?.let {
                    it.virtualFile == file
                }?: false
            }
        }
    }
}

