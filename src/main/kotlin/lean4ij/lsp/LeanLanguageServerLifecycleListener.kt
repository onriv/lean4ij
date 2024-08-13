package lean4ij.lsp

import lean4ij.project.LeanProjectService
import lean4ij.util.Constants
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerWrapper
import com.redhat.devtools.lsp4ij.ServerStatus
import com.redhat.devtools.lsp4ij.lifecycle.LanguageServerLifecycleListener
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.jsonrpc.MessageConsumer
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage

class LeanLanguageServerLifecycleListener(val project: Project) : LanguageServerLifecycleListener {
    private val leanProjectService : LeanProjectService = project.service()

    override fun handleStatusChanged(languageServer: LanguageServerWrapper) {
        if (languageServer.serverDefinition.id != Constants.LEAN_LANGUAGE_SERVER_ID) {
            return
        }
        if (languageServer.serverStatus == ServerStatus.none || languageServer.serverStatus == ServerStatus.stopped) {
            leanProjectService.resetServer()
        }
        // TODO maybe reset initializedServer to null also in here?
        if (languageServer.serverStatus == ServerStatus.started) {
            languageServer.initializedServer.thenAccept {
                leanProjectService.setInitializedServer(it)
            }
        }
    }

    override fun handleLSPMessage(message: Message, consumer: MessageConsumer, languageServer: LanguageServerWrapper) {
        if (languageServer.serverDefinition.id != Constants.LEAN_LANGUAGE_SERVER_ID) {
            return
        }
        (message as? ResponseMessage)?.let { (it.result as? InitializeResult)?.let {it2 ->
            leanProjectService.setInitializedResult(it2)
        }}
        (message as? NotificationMessage)?.let {
            // TODO for it seems no duplicated method can be defined for current version of lsp4j, we use
            //      listener here
            leanProjectService.emitServerEvent(message)
            if (it.method == "textDocument/publishDiagnostics") {
                val diagnostic = it.params as PublishDiagnosticsParams
                if (diagnostic.diagnostics.isNotEmpty()) {
                    leanProjectService.file(diagnostic.uri).publishDiagnostics(diagnostic.diagnostics)
                }
            }
        }
    }

    override fun handleError(p0: LanguageServerWrapper?, p1: Throwable?) {
    }

    override fun dispose() {
    }

}