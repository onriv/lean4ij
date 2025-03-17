package lean4ij.lsp.data

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import lean4ij.infoview.Lean4TextAttributesKeys
import lean4ij.infoview.dsl.*

/**
 * TODO this is different with the lean4 source code
 *      it's defined using the result shown in infoview-app
 *      check lean4/src/Lean/Widget/InteractiveGoal.lean:69:0
 *      where it's extends from InteractiveGoalCore
 */
class InteractiveTermGoal(
    val ctx: ContextInfo,
    val hyps: Array<InteractiveHypothesisBundle>,
    val range: Range,
    val term: ContextInfo,
    val type: TaggedText<SubexprInfo>
) {
    fun toInfoObjectModel(): InfoObjectModel = info {
        fold {
            h2("Expected type")
            // TODO duplicated
            for (hyp in hyps) {
                val names = hyp.names.joinToString(prefix = "", separator = " ", postfix = "")
                // TODO check if it's possible for using multiple text attributes
                val attr: MutableList<Lean4TextAttributesKeys> = mutableListOf()
                when {
                    hyp.isRemoved == true -> attr.add(Lean4TextAttributesKeys.RemovedText)
                    hyp.isInserted == true -> attr.add(Lean4TextAttributesKeys.InsertedText)
                }
                if (names.contains("✝")) {
                    attr.add(Lean4TextAttributesKeys.GoalInaccessible)
                } else {
                    attr.add(Lean4TextAttributesKeys.GoalHyp)
                }
                p(names, attr.map { it.key }.toMutableList())
                p(" : ")
                add(hyp.type.toInfoObjectModel())
                // TODO is it suitable doing line break this way?
                br()
            }
            p("⊢ ", Lean4TextAttributesKeys.SwingInfoviewGoalSymbol)
            add(type.toInfoObjectModel())
        }
    }

    /**
     * TODO this should absolutely DRY with [lean4ij.lsp.data.InteractiveGoals.getCodeText]
     */
    fun getCodeText(offset: Int): Triple<ContextInfo, Int, Int>? {
        for (hyp in hyps) {
            val type = hyp.type
            if (type.startOffset <= offset && offset < type.endOffset) {
                return type.getCodeText(offset, null)
            }
        }
        if (type.startOffset <= offset && offset < type.endOffset) {
            return type.getCodeText(offset, null)
        }
        return null
    }
}