package com.github.onriv.ijpluginlean.lsp.data

import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * TODO add doc
 * TODO this should driven from [RpcCallParams]]
 */
class InteractiveGoalsParams(
    val sessionId : String,
    val method: String,
    val params: PlainGoalParams,
    textDocument: TextDocumentIdentifier,
    position: Position
) : TextDocumentPositionParams(textDocument, position)