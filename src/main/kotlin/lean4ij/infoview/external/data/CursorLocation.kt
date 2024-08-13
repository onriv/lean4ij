package lean4ij.infoview.external.data

import lean4ij.lsp.data.Range

/**
 * for infoview, vscode's data structure
 */
data class CursorLocation(val uri: String, val range: Range)