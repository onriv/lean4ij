package lean4ij.lsp.data

/**
 * see [src/Lean/Widget/InteractiveGoal.lean#L106-L105](https://github.com/leanprover/lean4/blob/23e49eb519a45496a9740aeb311bf633a459a61e/src/Lean/Widget/InteractiveGoal.lean#L106-L105)
 */
class InteractiveGoals(
    val goals : List<InteractiveGoal>) {

    /**
     * This is from https://github.com/Julian/lean.nvim/blob/03f7437/lua/lean/infoview/components.lua
     * TODO implement the fold/open logic
     * TODO should this return a string?
     */
    fun toInfoViewString(sb: StringBuilder) {
        sb.append("▼ Tactic state\n")
        if (goals.isEmpty()) {
            sb.append("No goals\n")
            return
        }
        sb.append("${goals.size} goals\n")
        for (goal in goals) {
            goal.toInfoViewString(sb)
        }
    }

    /**
     * TODO add unittest for this and the above
     * TODO this should be DRY with [lean4ij.lsp.data.InteractiveTermGoal.getCodeText]
     */
    fun getCodeText(offset : Int) : CodeWithInfos? {
        for (goal in goals) {
            for (hyp in goal.hyps) {
                val type = hyp.type
                if (type.startOffset <= offset && offset < type.endOffset) {
                    return type.getCodeText(offset)
                }
            }
            if (goal.getStartOffset() <= offset && offset < goal.getEndOffset()) {
                return goal.getCodeText(offset)
            }
        }
        return null
    }
}