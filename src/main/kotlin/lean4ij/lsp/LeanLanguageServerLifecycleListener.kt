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
import java.lang.reflect.Proxy

/**
 * Intercepts LSP lifecycle events and messages for the Lean language server.
 *
 * This listener handles two categories of events:
 *
 * ## Server Status Changes ([handleStatusChanged])
 * - When the server stops or resets, clears the cached server reference in [LeanProjectService].
 * - When the server starts, captures the initialized server instance for RPC communication.
 *
 * ## LSP Message Interception ([handleLSPMessage])
 * Inspects raw LSP messages flowing between client and server to extract information that
 * is not directly available through the standard LSP4IJ API:
 *
 * - **Hover responses**: Tracks hover request/response pairs by message ID. When a hover
 *   response arrives, extracts the [Hover] result to drive the "highlight current content"
 *   feature in the editor.
 * - **InitializeResult**: Captures the server's initialization result, which is forwarded to
 *   the external (JCEF) infoview to signal that the server is ready.
 * - **Notification messages**: Forwards all server notifications (e.g., diagnostics, file
 *   progress) to [LeanProjectService] for event-driven processing. Also specifically handles
 *   `textDocument/publishDiagnostics` to update per-file diagnostic state.
 *
 * @param project The IntelliJ project this listener belongs to
 * @see LeanLanguageServerLifecycleListenerProxyFactory for why this uses dynamic proxying
 */
class LeanLanguageServerLifecycleListener(val project: Project) {
    private val leanProjectService: LeanProjectService = project.service()

    fun handleStatusChanged(languageServer: LanguageServerWrapper) {
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

    fun handleLSPMessage(message: Message, consumer: MessageConsumer, languageServer: LanguageServerWrapper) {
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

}

/**
 * Factory that creates a dynamic proxy implementing LSP4IJ's internal [LanguageServerLifecycleListener].
 *
 * LSP4IJ marks [LanguageServerLifecycleListener] as an internal API, meaning it cannot be
 * implemented directly without risking breakage across versions. This factory uses Java's
 * [Proxy] mechanism to create a compatible implementation at runtime, delegating calls to
 * [LeanLanguageServerLifecycleListener].
 *
 * ## Why this is needed
 * The Lean plugin requires access to lifecycle events that are not exposed through LSP4IJ's
 * public API:
 * - **Server status tracking**: To know when the server starts/stops for managing RPC sessions.
 * - **Message interception**: To capture hover responses for editor highlighting and to forward
 *   server notifications to the external infoview.
 *
 * ## Proxy Method Mapping
 * - `handleLSPMessage(Message, MessageConsumer, LanguageServerWrapper)` → delegates to target
 * - `handleStatusChanged(LanguageServerWrapper)` → delegates to target
 * - All other methods → no-op
 *
 * @see <a href="https://github.com/redhat-developer/lsp4ij/discussions/1324">LSP4IJ discussion</a>
 *   for the public API request
 * TODO remove this once LSP4IJ provides a public API for lifecycle events
 */
object LeanLanguageServerLifecycleListenerProxyFactory {
    fun create(project: Project): Any {
        val target = LeanLanguageServerLifecycleListener(project)
        val interfaceClass = Class.forName("com.redhat.devtools.lsp4ij.lifecycle.LanguageServerLifecycleListener")
        return Proxy.newProxyInstance(
            this::class.java.classLoader,
            // target::class.java.classLoader,
            arrayOf(interfaceClass)
        ) { proxy, method, args ->
            return@newProxyInstance when (method.name) {
                "handleLSPMessage" -> target.handleLSPMessage(args[0] as Message, args[1] as MessageConsumer, args[2] as LanguageServerWrapper)
                "handleStatusChanged" -> target.handleStatusChanged(args[0] as LanguageServerWrapper)
                else -> {

                }
            }
        }
    }
}