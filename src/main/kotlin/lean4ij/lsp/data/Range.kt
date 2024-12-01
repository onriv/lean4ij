package lean4ij.lsp.data

data class Range(val start: Position, val end: Position)

data class Target( val targetUri: String, val targetSelectionRange: Range, val targetRange: Range )