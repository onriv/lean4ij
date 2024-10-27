package lean4ij.lsp.data

/**
 * copy from src/Lean/Widget/InteractiveCode.lean:45
 * The polymorphism is achieved via class inheritance
 * TODO using Kotlin's property for getter/setter and avoid
 *      all fields being public
 * TODO this maybe should be renamed
 */
interface InfoViewContent {
    fun toInfoViewString(sb : InfoviewRender) : String
    fun contextInfo(offset: Int, startOffset: Int, endOffset : Int) : Triple<ContextInfo, Int, Int>?
    fun mayHighlight(sb: InfoviewRender, startOffset: Int, endOffset: Int) {}
}

/**
 * check src/Lean/Widget/TaggedText.lean 104 lines --16%--
 * This implementation still looses some information at the runtime,
 * which is not very convenient while debugging
 * maybe it can be reified, check
 * https://discuss.kotlinlang.org/t/reified-generics-on-class-level/16711/4
 */
abstract class TaggedText<T> where T: InfoViewContent {
    abstract fun toInfoViewString(sb: InfoviewRender, parent : TaggedText<T>?) : String

    @Transient
    var startOffset : Int = -1

    @Transient
    var endOffset : Int = -1

    @Transient
    var codeText : String = ""

    @Transient
    var parent : TaggedText<T>? = null

    /**
     * The `t` here is only use in TaggedTextText
     */
    abstract fun getCodeText(offset: Int, t: T?) : Triple<ContextInfo, Int, Int>?
}

class TaggedTextText<T>(val text: String) : TaggedText<T>() where T: InfoViewContent {
    override fun toInfoViewString(sb: InfoviewRender, parent : TaggedText<T>?): String {
        this.parent = parent
        startOffset = sb.length
        sb.append(text)
        endOffset = sb.length
        this.codeText = text
        return text
    }

    override fun getCodeText(offset: Int, t: T?): Triple<ContextInfo, Int, Int>? {
        // TODO find why here allow null
        if (t == null) return null
        return t!!.contextInfo(offset, parent!!.startOffset, parent!!.endOffset)
    }
}

class TaggedTextTag<T>(val f0: T, val f1: TaggedText<T>) : TaggedText<T>() where T: InfoViewContent {
    override fun toInfoViewString(sb: InfoviewRender, parent : TaggedText<T>?): String {
        this.parent = parent
        // TODO handle events
        startOffset = sb.length
        f0.toInfoViewString(sb)
        f1.toInfoViewString(sb, this)
        endOffset = sb.length
        f0.mayHighlight(sb, startOffset, endOffset)
        codeText = sb.substring(startOffset, endOffset)
        return codeText
    }

    override fun getCodeText(offset: Int, t: T?): Triple<ContextInfo, Int, Int>? {
        // TODO Here it must be very wrong for message
        return f1.getCodeText(offset, f0)
    }
}

class TaggedTextAppend<T>(private val append: List<TaggedText<T>>) : TaggedText<T>() where T: InfoViewContent {
    override fun toInfoViewString(sb: InfoviewRender, parent: TaggedText<T>?) : String {
        this.parent = parent
        this.startOffset = sb.length
        for (c in append) {
            c.toInfoViewString(sb, this)
        }
        this.endOffset = sb.length
        this.codeText = sb.substring(startOffset, endOffset)
        return this.codeText
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
    override fun toInfoViewString(sb: InfoviewRender): String {
        return expr.toInfoViewString(sb, null)
    }

    override fun contextInfo(offset: Int, startOffset: Int, endOffset : Int) : Triple<ContextInfo, Int, Int>? {
        return expr.getCodeText(offset, null)
    }
}

class MsgEmbedGoal(val goal: InteractiveGoal) : MsgEmbed() {
    override fun toInfoViewString(sb: InfoviewRender): String {
        return goal.toInfoViewString(sb, false)
    }

    override fun contextInfo(offset: Int, startOffset: Int, endOffset : Int) : Triple<ContextInfo, Int, Int>? {
        // TODO when does this happen?
        //      This happened on MIL/C07/S03_Subojects.lean:148
        TODO("Not yet implemented")
    }
}

/**
 * TODO trace! Not implement yet, check src/Lean/Widget/InteractiveDiagnostic.lean" 221 lines --11%--
 *      the definition of MsgEmbed
 */
abstract class MsgEmbedTrace(val indent: Int, val cls: String): MsgEmbed()

class MsgUnsupported(val message: String) : MsgEmbed() {
    override fun toInfoViewString(sb: InfoviewRender): String {
        sb.append(message)
        return message
    }

    override fun contextInfo(offset: Int, startOffset: Int, endOffset: Int): Triple<ContextInfo, Int, Int>? {
        return null
    }
}
