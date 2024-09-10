package lean4ij.lsp.data

/**
 * from src/Lean/Widget/InteractiveGoal.lean:51
 */
data class InteractiveHypothesisBundle(
    val names: List<String>,
    val fvarIds: List<String>,
    val type: TaggedText<SubexprInfo>,
    val value: TaggedText<SubexprInfo>? = null,
    val isInstance: Boolean? = null,
    val isType: Boolean? = null,
    val isInserted: Boolean? = null,
    val isRemoved: Boolean? = null
)