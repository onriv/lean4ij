package com.github.onriv.ijpluginlean.lsp.data

/**
 * see [src/Lean/Widget/InteractiveCode.lean#L44](https://github.com/leanprover/lean4/blob/1a12f63f742a578a514536ef45cf9df0c9793af0/src/Lean/Widget/InteractiveCode.lean#L44)
 */
class CodeWithInfosText (val text: String) : CodeWithInfos() {

    override fun toInfoViewString(startOffset: Int, parent: CodeWithInfos?) : String {
        this.parent = parent
        this.startOffset = startOffset
        this.endOffset = startOffset+text.length
        this.codeText = text
        return text
    }

    override fun getCodeText(offset: Int) : CodeWithInfos? {
        return this
    }
}