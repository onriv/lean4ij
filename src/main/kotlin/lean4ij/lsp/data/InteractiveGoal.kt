package lean4ij.lsp.data

class InteractiveGoal(
    val userName: String? = null,
    val type: CodeWithInfos,
    val mvarId: String,
    val isInserted: Boolean? = null,
    val hyps: Array<InteractiveHypothesisBundle>,
    val ctx: ContextInfo,
    val goalPrefix: String,
    val isRemoved: Boolean? = null) {

    @Transient
     private var startOffset : Int = -1

    @Transient
    private var endOffset : Int = -1

    /**
     * TODO maybe it's nice to add hyperlink logic here
     * TODO refactor StringBuilder into a Render
     */
     fun toInfoViewString(sb : StringBuilder)  {
         if (userName != null) {
             sb.append("case $userName\n")
         }
        // TODO deduplicate
         for (hyp in hyps) {
             val names = hyp.names.joinToString(prefix = "", separator = " ", postfix = " : ")
             sb.append(names)
             hyp.type.toInfoViewString(sb, null)
             sb.append("\n")
         }
         sb.append("⊢ ")
         type.toInfoViewString(sb, null)
         sb.append("\n")
         this.endOffset = startOffset+sb.count()
     }

    /**
     * TODO try DIY this
     * TODO kotlin way to do this
     */
    fun getStartOffset() = startOffset

    /**
     * TODO kotlin way to do this
     */
    fun getEndOffset() = endOffset

    fun getCodeText(offset: Int) : CodeWithInfos? {
        return type.getCodeText(offset)
    }
}