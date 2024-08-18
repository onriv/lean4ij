package lean4ij.lsp.data

import lean4ij.util.Constants
import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * TODO this class is also defined using data from real cases from infoview-app
 *      check lean4's source code
 */
class InteractiveDiagnosticsParams(
    sessionId : String,
    val params: LineRange,
    textDocument: TextDocumentIdentifier,
    position: Position
): RpcCallParams(sessionId, Constants.RPC_METHOD_GET_INTERACTIVE_DIAGNOSTICS, textDocument, position)

data class InteractiveDiagnostics(
    val fullRange: Range,
    val message: Message,
    val range: Range,
    val severity: Int,
    val source : String,
)