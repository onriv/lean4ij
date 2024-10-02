package lean4ij.lsp.data

import io.kinference.ndarray.extensions.inferType
import lean4ij.util.Constants
import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * This data is not from lean4 source code but currently, using the debug information from infoview-app
 */
data class LineRangeParam(val lineRange: LineRange)

/**
 * TODO this class is also defined using data from real cases from infoview-app
 *      check lean4's source code
 * TODO accidentally found that maybe set [params] to null gives all interactive diagnostics information
 */
class InteractiveDiagnosticsParams(
    sessionId : String,
    val params: LineRangeParam,
    textDocument: TextDocumentIdentifier,
    position: Position
): RpcCallParams(sessionId, Constants.RPC_METHOD_GET_INTERACTIVE_DIAGNOSTICS, textDocument, position)

data class InteractiveDiagnostics(
    val fullRange: Range,
    val message: TaggedText<MsgEmbed>,
    val range: Range,
    val severity: Int,
    val source : String,
) {
    fun toInfoViewString(interactiveInfoBuilder: InfoviewRender) {
        message.toInfoViewString(interactiveInfoBuilder, null)
    }
}