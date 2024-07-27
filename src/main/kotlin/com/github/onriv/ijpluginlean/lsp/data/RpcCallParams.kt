package com.github.onriv.ijpluginlean.lsp.data

import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * `$/lean/rpc/call` client->server request.
 *
 * TODO add doc to the lean source
 *
 * Although the lean source say it's Uint64 but here it's String...
 */
open class RpcCallParams<ParamsType >(
    val sessionId : String,
    val method: String,
    val params: ParamsType,
    textDocument: TextDocumentIdentifier,
    position: Position
) : TextDocumentPositionParams(textDocument, position)