package lean4ij.lsp.data

import lean4ij.infoview.Lean4TextAttributesKeys
import lean4ij.infoview.dsl.InfoObjectModel
import lean4ij.infoview.dsl.info

/**
 * Represents an interactive goal from the Lean 4 language server's `getInteractiveGoals` response.
 *
 * An interactive goal contains rich, structured data about the current proof state at a
 * cursor position. Unlike the plain-text `PlainGoal`, interactive goals carry:
 * - **Hypotheses** ([hyps]): Each hypothesis bundle includes names, types (as [TaggedText]
 *   with subexpression info for hover interaction), and diff markers (inserted/removed).
 * - **Goal type** ([type]): The target type to prove, as interactive tagged text.
 * - **Context info** ([ctx]): Information about the proof context for hover-to-inspect.
 * - **Goal prefix** ([goalPrefix]): Display prefix (e.g., "⊢" or "case").
 * - **Case name** ([userName]): For named cases, enables folding in the infoview.
 *
 * ## Rendering
 * [toInfoObjectModel] converts this goal into the DSL tree used by the internal (Swing) infoview.
 * Named cases are wrapped in a foldable region; unnamed goals render flat.
 *
 * @see InteractiveGoals for the container holding multiple goals
 * @see TaggedText for the rich text format with subexpression hover support
 */
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