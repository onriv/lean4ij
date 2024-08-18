package lean4ij.lsp.data

import lean4ij.util.Constants
import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * TODO this seems inconsistent with the lean source code
 *      check lean4/src/Lean/Server/FileWorker/WidgetRequests.lean:49:27
 *      this seems copied from lean.vim
 */
class InteractiveInfoParams(
    sessionId : String,
    textDocument: TextDocumentIdentifier,
    position: Position,
    val params: ContextInfo,
) : RpcCallParams(sessionId, Constants.RPC_METHOD_INFO_TO_INTERACTIVE, textDocument, position)