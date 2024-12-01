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
     * TODO maybe it's nice to add hyperlink logic here
     * TODO refactor StringBuilder into a Render
     *      all render logic should be refactored, it's inelegant and error prone
     */
    fun toInfoViewString(sb: InfoviewRender, haveMultiGoals: Boolean): String {
        val header = "case $userName"
        val start = sb.length
        if (userName != null) {
            sb.append(header, Lean4TextAttributesKeys.SwingInfoviewCasePos)
            sb.append('\n')
        }
        // TODO deduplicate DRY DRY DRY
        for (hyp in hyps) {
            val start = sb.length
            val names = hyp.names.joinToString(prefix = "", separator = " ", postfix = "")
            sb.append(names)
            when {
                hyp.isRemoved == true -> sb.highlight(start, sb.length, Lean4TextAttributesKeys.RemovedText)
                hyp.isInserted == true -> sb.highlight(start, sb.length, Lean4TextAttributesKeys.InsertedText)
            }
            if (names.contains("✝")) {
                sb.highlight(start, sb.length, Lean4TextAttributesKeys.GoalInaccessible)
            } else {
                sb.highlight(start, sb.length, Lean4TextAttributesKeys.GoalHyp)
            }
            sb.append(" : ")
            hyp.type.toInfoViewString(sb, null)
            sb.append("\n")
        }
        sb.append("⊢", Lean4TextAttributesKeys.SwingInfoviewGoalSymbol)
        sb.append(" ")
        // here startOffset and endOffset only for the goal
        this.startOffset = sb.length
        type.toInfoViewString(sb, null)
        this.endOffset = sb.length
        val end = sb.length
        if (haveMultiGoals) {
            sb.addFoldingOperation(start, end, header)
        }
        sb.append("\n")
        return sb.substring(startOffset, endOffset)
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