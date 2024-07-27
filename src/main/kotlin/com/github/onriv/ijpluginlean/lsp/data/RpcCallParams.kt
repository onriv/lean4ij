package com.github.onriv.ijpluginlean.lsp.data

import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * `$/lean/rpc/call` client->server request.
 *
 * TODO add doc to the lean source
 *
 * Although the lean source say it's Uint64 but here it's String...
 */
class RpcCallParams(
    val sessionId : String,
    val method: String,
    val params: Any,
    textDocument: TextDocumentIdentifier,
    position: Position
) : TextDocumentPositionParams(textDocument, position)