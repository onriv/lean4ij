package lean4ij.lsp

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerEnablementSupport
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
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

}