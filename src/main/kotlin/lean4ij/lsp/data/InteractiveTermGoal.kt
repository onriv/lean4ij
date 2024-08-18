package lean4ij.lsp.data

/**
 * TODO this is different with the lean4 source code
 *      it's defined using the result shown in infoview-app
 *      check lean4/src/Lean/Widget/InteractiveGoal.lean:69:0
 *      where it's extends from InteractiveGoalCore
 */
class InteractiveTermGoal(
    val ctx : ContextInfo,
    val hyps: Array<InteractiveHypothesisBundle>,
    val range: Range,
    val term: ContextInfo,
    val type: CodeWithInfos
) {
    /**
     * TODO refactor StringBuilder into a Render
     */
    fun toInfoViewString(sb: StringBuilder) {
        sb.append("▼ Expected type\n")
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
    }
}