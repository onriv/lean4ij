package com.github.onriv.ijpluginlean.lsp.data

/**
 * see [src/Lean/Widget/TaggedText.lean#L23](https://github.com/leanprover/lean4/blob/23e49eb519a45496a9740aeb311bf633a459a61e/src/Lean/Widget/TaggedText.lean#L23)
 */
class CodeWithInfosAppend (val append: List<CodeWithInfos>) : CodeWithInfos() {
    override fun toInfoViewString(startOffset: Int, parent: CodeWithInfos?) : String {
        this.parent = parent
        this.startOffset = startOffset
        val sb = StringBuilder()
        for (c in append) {
            sb.append(c.toInfoViewString(startOffset+sb.length, this))
        }
        this.endOffset=startOffset+sb.length
        this.codeText = sb.toString()
        return this.codeText
    }

    override fun getCodeText(offset: Int): CodeWithInfos? {
        for (c in append) {
            if (c.startOffset <= offset && offset < c.endOffset) {
                return c.getCodeText(offset)
            }
        }
        return null
    }
}