package com.github.onriv.ijpluginlean.lsp.data

import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * TODO DRY
 * TODO this should driven from [RpcCallParams]
 */
class InteractiveInfoParams(
    val sessionId : String,
    val method: String,
    val params: ContextInfo,
    textDocument: TextDocumentIdentifier,
    position: Position
) : TextDocumentPositionParams(textDocument, position)