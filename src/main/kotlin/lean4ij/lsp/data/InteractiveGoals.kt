package lean4ij.lsp.data

data class FoldingData(val startOffset: Int, val endOffset: Int, val placeholderText: String, val expanded: Boolean=true)


/**
 * TODO this may better be placed in the infoview package,
 *      but currently we added rich behaviors in these data classes
 */
class InfoviewRender(val sb: StringBuilder) {

    constructor(): this(StringBuilder())
    constructor(text: String): this(StringBuilder(text))

    fun append(text: String) {
        sb.append(text)
    }
    fun append(char: Char) {
        sb.append(char)
    }

    fun substring(start: Int, end: Int) : String {
        return sb.substring(start, end)
    }

    fun substring(start: Int) : String {
        return sb.substring(start)
    }

    override fun toString(): String {
        return sb.toString()
    }

    private val _foldings: MutableList<FoldingData> = mutableListOf()

    fun addFoldingOperation(startOffset: Int, endOffset: Int, placeholderText: String, expanded: Boolean=true) {
        _foldings.add(FoldingData(startOffset, endOffset, placeholderText, expanded))
    }

    fun deleteLastChar() {
        sb.deleteCharAt(sb.length - 1)
    }

    val length : Int get() = sb.length
    val foldings : List<FoldingData> get() = _foldings.toList()
}

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
    fun toInfoViewString(sb: InfoviewRender) {
        val header = "Tactic state"
        val start = sb.length
        sb.append("$header\n")
        if (goals.isEmpty()) {
            sb.append("No goals\n")
            return
        }
        val haveMultiGoals = goals.size > 1
        if (haveMultiGoals) {
            sb.append("${goals.size} goals\n")
        } else {
            sb.append("1 goal\n")
        }
        for (goal in goals) {
            goal.toInfoViewString(sb, haveMultiGoals)
        }
        // The last line break should not be folded, here the impl seems kind of adhoc
        sb.deleteLastChar()
        val end = sb.length
        sb.addFoldingOperation(start, end, header)
        sb.append('\n')
    }

    /**
     * TODO add unittest for this and the above
     * TODO this should be DRY with [lean4ij.lsp.data.InteractiveTermGoal.getCodeText]
     */
    fun getCodeText(offset : Int) : Triple<ContextInfo, Int, Int>? {
        for (goal in goals) {
            for (hyp in goal.hyps) {
                val type = hyp.type
                if (type.startOffset <= offset && offset < type.endOffset) {
                    return type.getCodeText(offset, null)
                }
            }
            if (goal.getStartOffset() <= offset && offset < goal.getEndOffset()) {
                return goal.getCodeText(offset)
            }
        }
        return null
    }
}