package com.github.onriv.ijpluginlean.lsp.data

import com.github.onriv.ijpluginlean.lsp.LspConstants
import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * TODO DRY
 * TODO this should driven from [RpcCallParams]
 */
class InteractiveInfoParams(
    sessionId : String,
    params: ContextInfo,
    textDocument: TextDocumentIdentifier,
    position: Position
) : RpcCallParams<ContextInfo>(sessionId, LspConstants.RPC_METHOD_INFO_TO_INTERACTIVE, params, textDocument, position)