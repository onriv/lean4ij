package com.github.onriv.ijpluginlean.lsp.data

/**
 * see: tests/lean/interactive/run.lean:11
 */
data class SubexprInfo (val subexprPos: String, val info: ContextInfo, val diffStatus: String?)