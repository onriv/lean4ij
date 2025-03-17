package lean4ij.lsp.data

import lean4ij.infoview.Lean4TextAttributesKeys
import lean4ij.infoview.dsl.InfoObjectModel

/**
 * see: tests/lean/interactive/run.lean:11
 */
data class SubexprInfo (val subexprPos: String, val info: ContextInfo, val diffStatus: String?) : InfoViewContent {
    override fun contextInfo(offset: Int, startOffset: Int, endOffset : Int) : Triple<ContextInfo, Int, Int>? {
        return Triple(info, startOffset, endOffset)
    }

    override fun mayHighlight(sb: InfoviewRender, startOffset: Int, endOffset: Int) {
        when (diffStatus) {
            null -> {}
            "wasChanged" -> {
                sb.highlight(startOffset, endOffset, Lean4TextAttributesKeys.InsertedText)
            }
            "willChange" -> {
                sb.highlight(startOffset, endOffset, Lean4TextAttributesKeys.RemovedText)
            }
            "willDelete" -> {
                sb.highlight(startOffset, endOffset, Lean4TextAttributesKeys.RemovedText)
            }
            else -> {
                // should not be here
                TODO("diffStatus: $diffStatus for infoview change not defined")
            }
        }
    }

    /**
     * Here the type SubexprInfo mainly used as a parameter type for TaggedTextTag<SubexprInfo>,
     * and it determines showing the full type as inserted text or removed text or not.
     *
     * But for Kotlin/Java's generic type limitation, we cannot the method directly for the type `TaggedTextTag<SubexprInfo>`
     * Hence we return the attribute here and let [TaggedTextTag.toInfoObjectModel] check it.
     */
    override fun toInfoObjectModel(): InfoObjectModel = when (diffStatus) {
        null -> InfoObjectModel("")
        "wasChanged" -> InfoObjectModel("", Lean4TextAttributesKeys.InsertedText.key)
        "willChange" -> InfoObjectModel("", Lean4TextAttributesKeys.RemovedText.key)
        "willDelete" -> InfoObjectModel("", Lean4TextAttributesKeys.RemovedText.key)
        else -> TODO("diffStatus: $diffStatus for infoview change not defined")
    }.also {
        it.contextInfo = info
    }
}