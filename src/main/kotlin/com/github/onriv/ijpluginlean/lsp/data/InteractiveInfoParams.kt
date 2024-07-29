package com.github.onriv.ijpluginlean.lsp.data

import com.github.onriv.ijpluginlean.util.Constants
import org.eclipse.lsp4j.TextDocumentIdentifier

class InteractiveInfoParams(
    sessionId : String,
    textDocument: TextDocumentIdentifier,
    position: Position,
    val params: ContextInfo,
) : RpcCallParams(sessionId, Constants.RPC_METHOD_INFO_TO_INTERACTIVE, textDocument, position)