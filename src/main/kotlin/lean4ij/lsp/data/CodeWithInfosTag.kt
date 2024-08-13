package lean4ij.lsp.data

/**
 * see: [src/Lean/Widget/TaggedText.lean#L24](https://github.com/leanprover/lean4/blob/23e49eb519a45496a9740aeb311bf633a459a61e/src/Lean/Widget/TaggedText.lean#L24)
 */
class CodeWithInfosTag (val f0: SubexprInfo, val f1: CodeWithInfos) : CodeWithInfos() {
    override fun toInfoViewString(startOffset: Int, parent: CodeWithInfos?) : String {
        this.parent = parent
        // TODO handle events
        this.startOffset = startOffset
        this.codeText = f1.toInfoViewString(startOffset, this)
        this.endOffset = startOffset+this.codeText.length
        return this.codeText
    }

    override fun getCodeText(offset: Int): CodeWithInfos? {
        return f1.getCodeText(offset)
    }
}