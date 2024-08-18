package lean4ij.lsp.data

import lean4ij.util.Constants
import org.eclipse.lsp4j.TextDocumentIdentifier

class InteractiveTermGoalParams(
    sessionId : String,
    val params: PlainGoalParams,
    textDocument: TextDocumentIdentifier,
    position: Position
) : RpcCallParams(sessionId, Constants.RPC_METHOD_GET_INTERACTIVE_TERM_GOAL, textDocument, position)

