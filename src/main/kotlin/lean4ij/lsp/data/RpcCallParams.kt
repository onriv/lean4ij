package lean4ij.lsp.data

import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * `$/lean/rpc/call` client->server request.
 *
 * TODO add doc to the lean source
 *
 * Although the lean source say it's Uint64 but here it's String...
 */
open class RpcCallParams(
    var sessionId : String,
    val method: String,
    textDocument: TextDocumentIdentifier,
    position: Position
) : TextDocumentPositionParams(textDocument, position)