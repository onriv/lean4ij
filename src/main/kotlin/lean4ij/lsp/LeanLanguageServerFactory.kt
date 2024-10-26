package lean4ij.lsp

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.redhat.devtools.lsp4ij.LanguageServerEnablementSupport
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The language server factory as LSP4IJ describe
 */
class LeanLanguageServerFactory : LanguageServerFactory, LanguageServerEnablementSupport {

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
        return isEnable.get()
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
        return object : LSPClientFeatures() {
            init {
                completionFeature = LeanLSPCompletionFeature()
            }
            // waiting for lsp4ij https://github.com/redhat-developer/lsp4ij/pull/586
            /*override*/ fun isEnabled(file: VirtualFile): Boolean {
                return FileEditorManager.getInstance(project).selectedTextEditor?.let {
                    it.virtualFile == file
                }?: false
            }
        }
    }
}

