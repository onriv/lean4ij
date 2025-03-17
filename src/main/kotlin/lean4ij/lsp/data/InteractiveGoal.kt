package lean4ij.lsp.data

import lean4ij.infoview.Lean4TextAttributesKeys
import lean4ij.infoview.dsl.InfoObjectModel
import lean4ij.infoview.dsl.info

class InteractiveGoal(
    val userName: String? = null,
    val type: TaggedText<SubexprInfo>,
    val mvarId: String,
    val isInserted: Boolean? = null,
    val hyps: Array<InteractiveHypothesisBundle>,
    val ctx: ContextInfo,
    val goalPrefix: String,
    val isRemoved: Boolean? = null) {

    /**
     * here startOffset and endOffset only for the goal
     */
    @Transient
    private var startOffset : Int = -1

    /**
     * here startOffset and endOffset only for the goal
     */
    @Transient
    private var endOffset : Int = -1

    fun toInfoObjectModel(): InfoObjectModel = info {
        if (userName != null) {
            fold {
                if (userName != null) {
                    h3("case $userName")
                }
                createGoalObjectModel(hyps, type)
            }
        } else {
            // if it has no userName, then do not allow folding it
            createGoalObjectModel(hyps, type)
        }
    }

    /**
     * TODO try DIY this
     * TODO kotlin way to do this, if possible treat it immutable
     *      check also [lean4ij.lsp.data.CodeWithInfos.startOffset]
     */
    fun getStartOffset() = startOffset

    /**
     * TODO kotlin way to do this
     */
    fun getEndOffset() = endOffset

    fun getCodeText(offset: Int) : Triple<ContextInfo, Int, Int>? {
        return type.getCodeText(offset, null)
    }
}