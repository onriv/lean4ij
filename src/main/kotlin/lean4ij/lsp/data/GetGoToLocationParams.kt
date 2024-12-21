package lean4ij.lsp.data

import lean4ij.util.Constants
import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * This is from an example of the request, not checking the source code of lean though
 * ```
 * {
 *     "method" : "Lean.Widget.getGoToLocation",
 *     "params" : {
 *         "kind" : "definition",
 *         "info" : {
 *             "p" : "183"
 *         }
 *     },
 *     "sessionId" : "10607226296505130896",
 *     "position" : {
 *         "line" : 1200,
 *         "character" : 21
 *     },
 *     "textDocument" : {
 *         "uri" : "file:///home/onriv/.elan/toolchains/leanprover--lean4---v4.11.0-rc2/src/lean/Init/Core.lean"
 *     }
 * }
 * ```
 */
data class GetGoToLocationParamsParams(
    val kind: String,
    val info: ContextInfo
)

class GetGoToLocationParams(
    sessionId : String,
    textDocument: TextDocumentIdentifier,
    position: Position,
    val params: GetGoToLocationParamsParams,
) : RpcCallParams(sessionId, Constants.RPC_METHOD_GET_GOTO_LOCATION, textDocument, position)