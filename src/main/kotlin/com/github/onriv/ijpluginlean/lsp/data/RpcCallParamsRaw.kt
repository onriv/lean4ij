package com.github.onriv.ijpluginlean.lsp.data

import org.eclipse.lsp4j.TextDocumentIdentifier

class RpcCallParamsRaw(
    sessionId : String,
    method: String,
    textDocument: TextDocumentIdentifier,
    position: Position,
    val params: Any
) : RpcCallParams(sessionId, method, textDocument, position)