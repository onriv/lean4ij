package lean4ij.lsp.data

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
            sb.append("${header}\n")
        }
        // TODO deduplicate
        for (hyp in hyps) {
            val names = hyp.names.joinToString(prefix = "", separator = " ", postfix = " : ")
            sb.append(names)
            hyp.type.toInfoViewString(sb, null)
            sb.append("\n")
        }
        sb.append("⊢ ")
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