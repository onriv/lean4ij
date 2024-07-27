package com.github.onriv.ijpluginlean.lsp.data

import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * see: [src/Lean/Data/Lsp/Extra.lean#L69](https://github.com/leanprover/lean4/blob/23e49eb519a45496a9740aeb311bf633a459a61e/src/Lean/Data/Lsp/Extra.lean#L69)
 */
data class FileProgressProcessingInfo(val textDocument: TextDocumentIdentifier, val processing: List<ProcessingInfo>)