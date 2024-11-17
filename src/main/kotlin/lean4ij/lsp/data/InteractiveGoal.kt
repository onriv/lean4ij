package lean4ij.lsp.data

import lean4ij.infoview.TextAttributesKeys

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

    /**
     * TODO maybe it's nice to add hyperlink logic here
     * TODO refactor StringBuilder into a Render
     *      all render logic should be refactored, it's inelegant and error prone
     */
    fun toInfoViewString(sb: InfoviewRender, haveMultiGoals: Boolean): String {
        val header = "case $userName"
        val start = sb.length
        if (userName != null) {
            sb.append(header, TextAttributesKeys.SwingInfoviewCasePos)
            sb.append('\n')
        }
        // TODO deduplicate DRY DRY DRY
        for (hyp in hyps) {
            val start = sb.length
            val names = hyp.names.joinToString(prefix = "", separator = " ", postfix = "")
            sb.append(names)
            when {
                hyp.isRemoved == true -> sb.highlight(start, sb.length, TextAttributesKeys.RemovedText)
                hyp.isInserted == true -> sb.highlight(start, sb.length, TextAttributesKeys.InsertedText)
            }
            if (names.contains("✝")) {
                sb.highlight(start, sb.length, TextAttributesKeys.GoalInaccessible)
            } else {
                sb.highlight(start, sb.length, TextAttributesKeys.GoalHyp)
            }
            sb.append(" : ")
            hyp.type.toInfoViewString(sb, null)
            sb.append("\n")
        }
        sb.append("⊢", TextAttributesKeys.SwingInfoviewGoalSymbol)
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