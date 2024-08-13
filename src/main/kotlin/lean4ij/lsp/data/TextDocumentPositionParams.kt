package lean4ij.lsp.data

import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * copy from [lean4/src/Lean/Data/Lsp/Basic.lea](https://github.com/leanprover/lean4/blob/3c82f9ae1208dc842d59755d5674aec37eda5ba0/src/Lean/Data/Lsp/Basic.lean#L336),
 * we don't need to define [TextDocumentIdentifier] as [lean4/src/Lean/Data/Lsp/Basic.lean#L165](https://github.com/leanprover/lean4/blob/3c82f9ae1208dc842d59755d5674aec37eda5ba0/src/Lean/Data/Lsp/Basic.lean#L165)
 * since it's contained in lsp4j
 */
open class TextDocumentPositionParams (
    val textDocument : TextDocumentIdentifier,
    val position : Position
)