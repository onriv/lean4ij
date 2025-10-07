package lean4ij.lsp

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerWrapper
import com.redhat.devtools.lsp4ij.ServerStatus
import com.redhat.devtools.lsp4ij.lifecycle.LanguageServerLifecycleListener
import lean4ij.project.LeanProjectService
import lean4ij.util.Constants
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.jsonrpc.MessageConsumer
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage
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

    private val hoverRequests = mutableSetOf<String>()

    override fun handleLSPMessage(message: Message, consumer: MessageConsumer, languageServer: LanguageServerWrapper) {
        if (languageServer.serverDefinition.id != Constants.LEAN_LANGUAGE_SERVER_ID) {
            return
        }
        if (message is RequestMessage) {
            if (message.params is HoverParams) {
               hoverRequests.add(message.id)
            }
        }
        if (message is ResponseMessage) {
            if (message.result is InitializeResult) {
                leanProjectService.setInitializedResult(message.result as InitializeResult)
            }
            if (message.id in hoverRequests) {
                // get current hover results for showing highlight of current content
                hoverRequests.remove(message.id)
                // TODO here I got a cast Exception earlier, but
                //      but unable to reproduce it in develop environment yet
                //      Checking LSP the message.result do be a Hover object
                //      in org.eclipse.lsp4j.services.TextDocumentService.hover
                //      So why could it fail to cast to Hover?
                try {
                    leanProjectService.highlightCurrentContent(message.result as Hover?)
                } catch (e: Exception) {
                    thisLogger().error("Failed to cast message.result to Hover with result ${message.result}", e)
                    throw e
                }
            }
            // This is not customize used in yet
            // if (message.result is SemanticTokens) {
            // }
        }
        (message as? NotificationMessage)?.let {
            // TODO for it seems no duplicated method can be defined for current version of lsp4j, we use
            //      listener here
            leanProjectService.emitServerEvent(message)
            if (it.method == "textDocument/publishDiagnostics") {
                val diagnostic = it.params as PublishDiagnosticsParams
                leanProjectService.file(diagnostic.uri).publishDiagnostics(diagnostic)
            }
        }
    }

    override fun handleError(p0: LanguageServerWrapper, p1: Throwable) {
    }

    override fun dispose() {
    }

}
