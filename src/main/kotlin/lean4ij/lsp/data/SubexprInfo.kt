package lean4ij.lsp.data

import lean4ij.infoview.TextAttributesKeys

/**
 * see: tests/lean/interactive/run.lean:11
 */
data class SubexprInfo (val subexprPos: String, val info: ContextInfo, val diffStatus: String?) : InfoViewContent {
    override fun toInfoViewString(sb: InfoviewRender): String {
        // TODO SubexprInfo seems totally independent with render?
        return ""
    }

    override fun contextInfo(offset: Int, startOffset: Int, endOffset : Int) : Triple<ContextInfo, Int, Int>? {
        return Triple(info, startOffset, endOffset)
    }

    override fun mayHighlight(sb: InfoviewRender, startOffset: Int, endOffset: Int) {
        when (diffStatus) {
            null -> {}
            "wasChanged" -> {
                sb.highlight(startOffset, endOffset, TextAttributesKeys.InsertedText)
            }
            "willChange" -> {
                sb.highlight(startOffset, endOffset, TextAttributesKeys.RemovedText)
            }
            "willDelete" -> {
                sb.highlight(startOffset, endOffset, TextAttributesKeys.RemovedText)
            }
            else -> {
                // should not be here
                TODO("diffStatus: $diffStatus for infoview change not defined")
            }
        }
    }
}