package lean4ij.lsp.data

/**
 * see [src/Lean/Widget/InteractiveGoal.lean#L106-L105](https://github.com/leanprover/lean4/blob/23e49eb519a45496a9740aeb311bf633a459a61e/src/Lean/Widget/InteractiveGoal.lean#L106-L105)
 */
class InteractiveGoals(
    val goals : List<InteractiveGoal>) {

    /**
     * This is from https://github.com/Julian/lean.nvim/blob/03f7437/lua/lean/infoview/components.lua
     */
    fun toInfoViewString(sb: StringBuilder) : String {
        if (goals.isEmpty()) {
            sb.append("goals accomplished 🎉\n")
            return sb.toString()
        }
        sb.append("${goals.size} goals\n")
        for (goal in goals) {
            sb.append(goal.toInfoViewString(sb.length))
        }
        return sb.toString()
    }

    /**
     * TODO add unittest for this and the above
     */
    fun getCodeText(offset : Int) : CodeWithInfos? {
        for (goal in goals) {
            if (goal.getStartOffset() <= offset && offset < goal.getEndOffset()) {
                return goal.getCodeText(offset)
            }
        }
        return null
    }
}