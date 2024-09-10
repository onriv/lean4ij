package lean4ij.lsp.data

/**
 * see: tests/lean/interactive/run.lean:11
 */
data class SubexprInfo (val subexprPos: String, val info: ContextInfo, val diffStatus: String?) : InfoViewRenderer {
    override fun toInfoViewString(sb: StringBuilder): String {
        // TODO SubexprInfo seems totally independent with render?
        return ""
    }

    override fun contextInfo(offset: Int, startOffset: Int, endOffset : Int) : Triple<ContextInfo, Int, Int>? {
        return Triple(info, startOffset, endOffset)
    }

}