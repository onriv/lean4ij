package lean4ij.lsp.data

import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import lean4ij.infoview.Lean4TextAttributesKeys
import lean4ij.infoview.dsl.*

/**
 * A quite naive implementation for an interval tree? Some code like
 * [com.intellij.openapi.editor.impl.IntervalTreeImpl] in Intellij
 * may help
 * TODO The name maybe not suitable since it's not an interval tree maybe
 */
class IntervalTree<T>(
    private val start: Int,
    private val end: Int,
    private val elem: T,
    private val childrens: MutableList<IntervalTree<T>>
) {

    fun getElemAt(offset: Int) : T? {
        return getNodeAt(offset)?.elem
    }

    private fun getNodeAt(offset: Int) : IntervalTree<T>? {
        if (offset !in start..<end) {
            return null
        }
        for (child in childrens) {
            child.getNodeAt(offset)?.let { return it }
        }
        return this
    }

    fun insertRange(start: Int, end: Int, elem: T) {
        val node = getNodeAt(start)
        // here we make a contract: always construct a large enough tree node first
        // and hence node would not be null
        node!!.childrens.add(IntervalTree(start, end, elem, mutableListOf()))
    }

}


// TODO clean startOffset and endOffset and isAllMessages and placeholderText fields from it
data class FoldingData(val startOffset: Int, val endOffset: Int, val placeholderText: String, val expanded: Boolean=true,
                       // this is to denote that the folding is for "All Messages"
                       // TODO it's very adhoc and blur define it here, maybe some better way to do this
                       // TODO cannot be removed... it's used for avoiding wrongly collapsed
                       val isAllMessages: Boolean=false,
                       val listener: ((FoldRegion)->Unit)?=null
    )
data class HighlightData(val startOffset: Int, val endOffset: Int, val textAttributes: TextAttributes)


/**
 * TODO this may better be placed in the infoview package,
 *      but currently we added rich behaviors in these data classes
 */
class InfoviewRender(private val sb: StringBuilder, val project: Project?, val file: VirtualFile?) {

    constructor(project: Project?, file: VirtualFile?): this(StringBuilder(), project, file)
    constructor(text: String, project: Project?, file: VirtualFile?): this(StringBuilder(text), project, file)

    fun append(text: String) {
        sb.append(text)
    }

    /**
     * append with highlight
     */
    fun append(text: String, key: Lean4TextAttributesKeys) {
        val start = sb.length
        sb.append(text)
        highlight(start, sb.length, key)
    }

    fun append(char: Char) {
        sb.append(char)
    }

    /**
     * TODO exception here:
     *      Exception in thread "DefaultDispatcher-worker-36 @lean4ij.project.LeanProjectService#29011" java.lang.StringIndexOutOfBoundsException: start 902, end 96, length 96
     *      	at java.base/java.lang.AbstractStringBuilder.checkRangeSIOOBE(AbstractStringBuilder.java:1810)
     *      	at java.base/java.lang.AbstractStringBuilder.substring(AbstractStringBuilder.java:1070)
     *      	at java.base/java.lang.StringBuilder.substring(StringBuilder.java:91)
     *      	at lean4ij.lsp.data.InfoviewRender.substring(InteractiveGoals.kt:42)
     *      	at lean4ij.lsp.data.TaggedTextAppend.toInfoViewString(CodeWithInfos.kt:88)
     *      	at lean4ij.lsp.data.MsgEmbedExpr.toInfoViewString(CodeWithInfos.kt:106)
     *      	at lean4ij.lsp.data.TaggedTextTag.toInfoViewString(CodeWithInfos.kt:66)
     *      	at lean4ij.infoview.LeanInfoViewWindowFactory$Companion.updateInteractiveGoal(LeanInfoViewWindowFactory.kt:116)
     *      	at lean4ij.project.LeanFile$updateInternalInfoview$1.invokeSuspend(LeanFile.kt:249)
     *      	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith$$$capture(ContinuationImpl.kt:33)
     *      	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt)
     *      	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:108)
     *      	at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:584)
     *      	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:793)
     *      	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:697)
     *      	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:684)
     */
    fun substring(start: Int, end: Int) : String {
        try {
            return sb.substring(start, end)
        } catch (e: IndexOutOfBoundsException) {
            // TODO mostly happen when errors
            return ""
        }
    }

    fun substring(start: Int) : String {
        return sb.substring(start)
    }

    override fun toString(): String {
        return sb.toString()
    }

    private val _foldings: MutableList<FoldingData> = mutableListOf()
    private val _highlights: MutableList<HighlightData> = mutableListOf()

    /**
     * TODO maybe add some indent logic
     */
    fun addFoldingOperation(folding: FoldingData) {
        _foldings.add(folding)
    }

    fun addFoldingOperation(startOffset: Int, endOffset: Int, placeholderText: String, expanded: Boolean=true) {
        addFoldingOperation(FoldingData(startOffset, endOffset, placeholderText, expanded))
    }

    fun deleteLastChar() {
        sb.deleteCharAt(sb.length - 1)
    }

    fun highlight(startOffset: Int, endOffset: Int, textAttributes: TextAttributes) {
        _highlights.add(HighlightData(startOffset, endOffset, textAttributes))
    }

    fun highlight(startOffset: Int, endOffset: Int, key: TextAttributesKey) {
        return highlight(startOffset, endOffset, EditorColorsManager.getInstance().globalScheme.getAttributes(key))
    }

    fun highlight(startOffset: Int, endOffset: Int, key: Lean4TextAttributesKeys) {
        return highlight(startOffset, endOffset, EditorColorsManager.getInstance().globalScheme.getAttributes(key.key))
    }

    val length : Int get() = sb.length
    val foldings : List<FoldingData> get() = _foldings.toList()
    val highlights : List<HighlightData> get() = _highlights.toList()

    /**
     * TODO here a specific class should be defined for () -> Unit
     */
    private val clickActions : IntervalTree<(EditorMouseEvent) -> Unit> = IntervalTree(0, Int.MAX_VALUE, {}, mutableListOf())

    fun addClickAction(start: Int, end: Int, action: (EditorMouseEvent) -> Unit) {
        clickActions.insertRange(start, end, action)
    }

    fun getClickAction(offset: Int) : ((EditorMouseEvent) -> Unit)? {
        return clickActions.getElemAt(offset)
    }
}

/**
 * see [src/Lean/Widget/InteractiveGoal.lean#L106-L105](https://github.com/leanprover/lean4/blob/23e49eb519a45496a9740aeb311bf633a459a61e/src/Lean/Widget/InteractiveGoal.lean#L106-L105)
 */
class InteractiveGoals(
    val goals : List<InteractiveGoal>) {

    fun toInfoObjectModel(): InfoObjectModel = info {
        fold {
            h2("Tactic state")
            if (goals.isEmpty()) {
                +"No goals"
                return@fold
            }
            if (goals.size == 1) {
                +"1 goal"
            } else {
                +"${goals.size} goals"
            }
            br()
            for ((index, goal) in goals.withIndex()) {
                add(goal.toInfoObjectModel())
                if (index != goals.lastIndex) {
                    br()
                }
            }
        }
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