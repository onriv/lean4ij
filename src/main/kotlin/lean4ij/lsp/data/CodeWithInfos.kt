package lean4ij.lsp.data

import kotlinx.coroutines.launch
import lean4ij.infoview.Lean4TextAttributesKeys
import lean4ij.infoview.LeanInfoviewContext
import lean4ij.infoview.dsl.InfoObjectModel
import lean4ij.infoview.dsl.info
import lean4ij.util.LspUtil
import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * Interface for content that can be displayed in the InfoView.
 *
 * This mirrors the polymorphic nature of Lean's widget system.
 * Implementations (like [MsgEmbed] and [SubexprInfo]) provide logic to:
 * - Render themselves into the InfoView object model ([toInfoObjectModel]).
 * - Provide context info for specific offsets ([contextInfo]).
 * - Handle highlighting ([mayHighlight]).
 */
interface InfoViewContent {
    /**
     * Retrieves context information (e.g., type info, definition location) for a given offset.
     */
    fun contextInfo(offset: Int, startOffset: Int, endOffset: Int): Triple<ContextInfo, Int, Int>?

    /**
     * Applies highlighting to the InfoView content based on its state (e.g., diff status).
     */
    fun mayHighlight(sb: InfoviewRender, startOffset: Int, endOffset: Int) {}

    /**
     * Converts this content into the internal object model used for rendering the InfoView.
     */
    fun toInfoObjectModel(): InfoObjectModel
}

/**
 * A recursive data structure representing text tagged with metadata.
 *
 * It corresponds to `Lean.Widget.TaggedText` in Lean 4.
 *
 * Structure:
 * - [TaggedTextText]: A leaf node containing a raw string.
 * - [TaggedTextAppend]: A list of `TaggedText` nodes (concatenation).
 * - [TaggedTextTag]: A wrapper that applies a tag of type [T] to a child `TaggedText`.
 *
 * @param T The type of the tag (metadata), which must implement [InfoViewContent].
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
     * Recursively searches for the context info at the given offset.
     *
     * @param offset The absolute offset in the rendered text.
     * @param t The tag associated with the current node (passed down during recursion).
     */
    abstract fun getCodeText(offset: Int, t: T?): Triple<ContextInfo, Int, Int>?

    /**
     * Converts this node into the InfoView object model.
     */
    abstract fun toInfoObjectModel(): InfoObjectModel
}

/**
 * Leaf node representing raw text.
 */
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
        //      Logic: If the tag is an expression (MsgEmbedExpr), extract the inner SubexprInfo
        //      and check if the offset falls within its range.
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

/**
 * Node wrapping a child with a metadata tag [f0].
 */
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

/**
 * Node representing a sequence of [TaggedText] elements.
 */
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

/**
 * Base class for messages embedded in the InfoView.
 * Corresponds to `Lean.Widget.MsgEmbed`.
 */
abstract class MsgEmbed : InfoViewContent

/**
 * Represents an embedded expression.
 */
class MsgEmbedExpr(val expr: TaggedText<SubexprInfo>) : MsgEmbed() {
    override fun contextInfo(offset: Int, startOffset: Int, endOffset: Int): Triple<ContextInfo, Int, Int>? {
        return expr.getCodeText(offset, null)
    }

    override fun toInfoObjectModel(): InfoObjectModel = info {
        add(expr.toInfoObjectModel())
    }
}

/**
 * Represents an embedded goal state.
 */
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
 * Represents a trace message, which can be expanded lazily.
 *
 * @property indent Indentation level.
 * @property cls CSS class/category of the trace.
 * @property collapsed Initial state.
 * @property children Children of the trace, either strictly loaded or lazy (requiring RPC call).
 * @property msg The content of the trace message itself.
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

/**
 * Fallback for unsupported message types.
 */
class MsgUnsupported(val message: String) : MsgEmbed() {
    override fun contextInfo(offset: Int, startOffset: Int, endOffset: Int): Triple<ContextInfo, Int, Int>? {
        return null
    }

    override fun toInfoObjectModel(): InfoObjectModel = InfoObjectModel(message)
}

/**
 * Helper class for data that can be either strictly present [A] or lazily loaded [B].
 * Corresponds to the `StrictOrLazy` inductive type in Lean.
 */
abstract class StrictOrLazy<A, B>
data class StrictOrLazyStrict<A, B>(val strict: A) : StrictOrLazy<A, B>()
data class StrictOrLazyLazy<A, B>(val lazy: B) : StrictOrLazy<A, B>()
