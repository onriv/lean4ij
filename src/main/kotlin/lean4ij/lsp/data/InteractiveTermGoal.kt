package lean4ij.lsp.data

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor

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
    val type: TaggedText<SubexprInfo>
) {
    /**
     * TODO refactor StringBuilder into a Render
     */
    fun toInfoViewString(editor: Editor, sb: StringBuilder) {
        sb.append("▼ Expected type\n")
        val start = sb.length
        // TODO deduplicate
        for (hyp in hyps) {
            val names = hyp.names.joinToString(prefix = "", separator = " ", postfix = " : ")
            sb.append(names)
            hyp.type.toInfoViewString(sb, null)
            sb.append("\n")
        }
        sb.append("⊢ ")
        type.toInfoViewString(sb, null)
        val end = sb.length
        // TODO it can not add fold here directly for the content still not add to editor yet
        //
        // ApplicationManager.getApplication().invokeLater {
        //     editor.foldingModel.runBatchFoldingOperation {
        //         val region = editor.foldingModel.addFoldRegion(start, end, "TODO")
        //         region?.isExpanded = false
        //     }
        // }
        sb.append("\n")
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