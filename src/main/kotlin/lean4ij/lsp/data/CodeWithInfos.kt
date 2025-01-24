package lean4ij.lsp.data

import kotlinx.coroutines.launch
import lean4ij.infoview.Lean4TextAttributesKeys
import lean4ij.infoview.LeanInfoviewContext
import lean4ij.infoview.dsl.InfoObjectModel
import lean4ij.infoview.dsl.info
import lean4ij.util.LspUtil
import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * copy from src/Lean/Widget/InteractiveCode.lean:45
 * The polymorphism is achieved via class inheritance
 * TODO using Kotlin's property for getter/setter and avoid
 *      all fields being public
 * TODO this maybe should be renamed
 */
interface InfoViewContent {
    fun contextInfo(offset: Int, startOffset: Int, endOffset: Int): Triple<ContextInfo, Int, Int>?
    fun mayHighlight(sb: InfoviewRender, startOffset: Int, endOffset: Int) {}
    fun toInfoObjectModel(): InfoObjectModel
}

/**
 * check src/Lean/Widget/TaggedText.lean 104 lines --16%--
 * This implementation still looses some information at the runtime,
 * which is not very convenient while debugging
 * maybe it can be reified, check
 * https://discuss.kotlinlang.org/t/reified-generics-on-class-level/16711/4
 */
abstract class TaggedText<T> where T : InfoViewContent {
    @Transient
    var startOffset: Int = -1

    @Transient
    var endOffset: Int = -1

    @Transient
    var codeText: String = ""

    @Transient
    var parent: TaggedText<T>? = null

    /**
     * The `t` here is only use in TaggedTextText
     */
    abstract fun getCodeText(offset: Int, t: T?): Triple<ContextInfo, Int, Int>?

    abstract fun toInfoObjectModel(): InfoObjectModel
}

class TaggedTextText<T>(val text: String) : TaggedText<T>() where T : InfoViewContent {

    /**
     * TODO here there should be some hover information
     */
    override fun toInfoObjectModel(): InfoObjectModel = info {
        if (text == "declaration uses 'sorry'") {
            p(text, Lean4TextAttributesKeys.SwingInfoviewAllMessageSorryPos)
        } else {
            p(text)
        }
    }

    override fun getCodeText(offset: Int, t: T?): Triple<ContextInfo, Int, Int>? {
        // TODO find why here allow null
        if (t == null) return null
        // TODO the implementation currently is trying to make it generic and avoid type check,
        //      but as time pass it becomes hard to read
        (t as? MsgEmbedExpr)?.let {
            (it.expr as? TaggedTextTag<SubexprInfo>)?.let {
                if (it.startOffset <= offset && offset < it.endOffset) {
                    return it.getCodeText(offset, it.f0)
                }
                return null
            }
        }
        return t.contextInfo(offset, parent!!.startOffset, parent!!.endOffset)
    }
}

class TaggedTextTag<T>(val f0: T, val f1: TaggedText<T>) : TaggedText<T>() where T : InfoViewContent {

    override fun toInfoObjectModel(): InfoObjectModel = info {
        val model = f0.toInfoObjectModel()
        add(model)
        add(f1.toInfoObjectModel())
        addAttr(model.attr)
        setContextInfo(model.contextInfo)
    }

    override fun getCodeText(offset: Int, t: T?): Triple<ContextInfo, Int, Int>? {
        // TODO Here it must be very wrong for message
        return f1.getCodeText(offset, f0)
    }
}

class TaggedTextAppend<T>(private val append: List<TaggedText<T>>) : TaggedText<T>() where T : InfoViewContent {

    override fun toInfoObjectModel(): InfoObjectModel = info {
        for (c in append) {
            add(c.toInfoObjectModel())
        }
    }

    override fun getCodeText(offset: Int, t: T?): Triple<ContextInfo, Int, Int>? {
        for (c in append) {
            if (c.startOffset <= offset && offset < c.endOffset) {
                return c.getCodeText(offset, t)
            }
        }
        return null
    }
}

abstract class MsgEmbed : InfoViewContent

class MsgEmbedExpr(val expr: TaggedText<SubexprInfo>) : MsgEmbed() {
    override fun contextInfo(offset: Int, startOffset: Int, endOffset: Int): Triple<ContextInfo, Int, Int>? {
        return expr.getCodeText(offset, null)
    }

    override fun toInfoObjectModel(): InfoObjectModel = info {
        add(expr.toInfoObjectModel())
    }
}

class MsgEmbedGoal(val goal: InteractiveGoal) : MsgEmbed() {
    override fun contextInfo(offset: Int, startOffset: Int, endOffset: Int): Triple<ContextInfo, Int, Int>? {
        // TODO when does this happen? Mostly when error happens
        //      This happened on MIL/C07/S03_Subojects.lean:148
        TODO("Not yet implemented")
    }

    override fun toInfoObjectModel(): InfoObjectModel = info {
        add(goal.toInfoObjectModel())
    }
}

/**
 * TODO trace! Not implement yet, check src/Lean/Widget/InteractiveDiagnostic.lean" 221 lines --11%--
 *      the definition of MsgEmbed
 */
class MsgEmbedTrace(
    val indent: Int, val cls: String, private val collapsed: Boolean,
    // TODO children: json like { "lazy" : { "p" : "31" } }
    //      this is a little idle using a map, it should be some type, check
    //      src/Lean/Widget/InteractiveDiagnostic.lean:248
    // private val children : Map<String, ContextInfo>,
    private val children: StrictOrLazy<List<TaggedText<MsgEmbed>>, ContextInfo>,
    private val msg: TaggedTextAppend<MsgEmbed>
) : MsgEmbed() {

    private suspend fun lazyTraceChildrenToInteractive(context: LeanInfoviewContext): List<TaggedText<MsgEmbed>>? {
        val contextInfo = (children as? StrictOrLazyLazy)?.lazy ?: return null
        val position = context.position
        val file = context.file
        val leanProject = context.leanProject
        val leanFile = leanProject.file(file)
        val session = context.leanProject.file(context.file).getSession()
        val textDocument = TextDocumentIdentifier(LspUtil.quote(file.path))
        val rpcParams = LazyTraceChildrenToInteractiveParams(
            sessionId = session,
            params = contextInfo,
            textDocument = textDocument,
            position = position
        )
        return leanFile.lazyTraceChildrenToInteractive(rpcParams)
    }

    private suspend fun getLazyMsgEmbedTrace(context: LeanInfoviewContext, model: InfoObjectModel) {
        val lazyTrace = lazyTraceChildrenToInteractive(context) ?: return
        val br = InfoObjectModel("\n")
        // clean the click action
        for (child in model.children) {
            child.clickAction = null
            if (child.text == " ▶") {
                child.text = " ▼"
            }
        }
        model.children.add(br)
        for ((idx, trace) in lazyTrace.withIndex()) {
            model.children.add(trace.toInfoObjectModel())
            if (idx != lazyTrace.lastIndex) {
                model.children.add(br)
            }
        }
        context.refresh()
    }

    override fun contextInfo(offset: Int, startOffset: Int, endOffset: Int): Triple<ContextInfo, Int, Int>? {
        // TODO
        return null
        // TODO("Not yet implemented")
    }

    override fun toInfoObjectModel(): InfoObjectModel {
        val traceModel = info {
            if (children is StrictOrLazyLazy) {
                p("[$cls] ", mutableListOf( Lean4TextAttributesKeys.GoalInaccessible).map { it.key }.toMutableList()) { context, model ->
                    val parent = model.parent?:return@p
                    context.leanProject.scope.launch {
                        getLazyMsgEmbedTrace(context, parent)
                    }
                }
                add(msg.toInfoObjectModel())
                p(" ▶") { context, model ->
                    val parent = model.parent?:return@p
                    context.leanProject.scope.launch {
                        getLazyMsgEmbedTrace(context, parent)
                    }
                }
            }
            if (children is StrictOrLazyStrict) {
                p("[$cls] ", mutableListOf( Lean4TextAttributesKeys.GoalInaccessible).map { it.key }.toMutableList())
                add(msg.toInfoObjectModel())
            }
        }
        return traceModel
    }
}

class MsgUnsupported(val message: String) : MsgEmbed() {
    override fun contextInfo(offset: Int, startOffset: Int, endOffset: Int): Triple<ContextInfo, Int, Int>? {
        return null
    }

    override fun toInfoObjectModel(): InfoObjectModel = InfoObjectModel(message)
}

/**
 * class corresponding to the lean inductive with the same name
 * in Lean/Widget/InteractiveDiagnostic.lean:15
 * This is kind of same as Either
 * Strict or lazy may largely be used by kotlin
 */
abstract class StrictOrLazy<A, B>
data class StrictOrLazyStrict<A, B>(val strict: A) : StrictOrLazy<A, B>()
data class StrictOrLazyLazy<A, B>(val lazy: B) : StrictOrLazy<A, B>()
