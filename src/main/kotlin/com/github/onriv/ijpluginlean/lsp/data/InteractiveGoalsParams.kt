package com.github.onriv.ijpluginlean.lsp.data

import com.github.onriv.ijpluginlean.lsp.LspConstants
import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * TODO add doc
 * TODO this should driven from [RpcCallParams]
 */
class InteractiveGoalsParams(
    sessionId : String,
    params: PlainGoalParams,
    textDocument: TextDocumentIdentifier,
    position: Position
) : RpcCallParams<PlainGoalParams>(sessionId, LspConstants.RPC_METHOD_GET_INTERACTIVE_GOALS, params, textDocument, position)