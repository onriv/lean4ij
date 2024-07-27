package com.github.onriv.ijpluginlean.lsp.data

//data class InteractiveGoalCore(
//    val hyps: Array<InteractiveHypothesisBundle>,
//    val type: CodeWithInfos,
//    val ctx: ContextInfo
//)
//
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

     fun toInfoViewString(startOffset: Int) : String {
         this.startOffset = startOffset
         val sb = StringBuilder()
         if (userName != null) {
             sb.append("case $userName\n")
         }
         // TODO hyps
         sb.append("⊢ ")
         sb.append("${type.toInfoViewString(startOffset+sb.length, null)}\n")
         this.endOffset = startOffset+sb.count()
         return sb.toString()
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