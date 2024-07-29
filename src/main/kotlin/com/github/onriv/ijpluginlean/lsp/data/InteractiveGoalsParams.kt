package com.github.onriv.ijpluginlean.lsp.data

import com.github.onriv.ijpluginlean.util.Constants
import org.eclipse.lsp4j.TextDocumentIdentifier

class InteractiveGoalsParams(
    sessionId : String,
    val params: PlainGoalParams,
    textDocument: TextDocumentIdentifier,
    position: Position
) : RpcCallParams(sessionId, Constants.RPC_METHOD_GET_INTERACTIVE_GOALS, textDocument, position)