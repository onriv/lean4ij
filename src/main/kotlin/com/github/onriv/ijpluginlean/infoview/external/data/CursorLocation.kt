package com.github.onriv.ijpluginlean.infoview.external.data

import com.github.onriv.ijpluginlean.lsp.data.Range

/**
 * for infoview, vscode's data structure
 */
data class CursorLocation(val uri: String, val range: Range)