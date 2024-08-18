package lean4ij.lsp.data

/**
 * see [src/Lean/Widget/TaggedText.lean#L23](https://github.com/leanprover/lean4/blob/23e49eb519a45496a9740aeb311bf633a459a61e/src/Lean/Widget/TaggedText.lean#L23)
 */
class CodeWithInfosAppend (private val append: List<CodeWithInfos>) : CodeWithInfos() {
    override fun toInfoViewString(sb : StringBuilder, parent: CodeWithInfos?): String {
        this.parent = parent
        this.startOffset = sb.length
        for (c in append) {
            c.toInfoViewString(sb, this)
        }
        this.endOffset = sb.length
        this.codeText = sb.substring(startOffset, endOffset)
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