package lean4ij.lsp.data

import lean4ij.util.Constants
import org.eclipse.lsp4j.TextDocumentIdentifier

class InteractiveGoalsParams(
    sessionId : String,
    val params: PlainGoalParams,
    textDocument: TextDocumentIdentifier,
    position: Position
) : RpcCallParams(sessionId, Constants.RPC_METHOD_GET_INTERACTIVE_GOALS, textDocument, position)