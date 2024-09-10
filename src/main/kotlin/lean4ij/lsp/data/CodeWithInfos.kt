package lean4ij.lsp.data

import com.jetbrains.rd.framework.base.deepClonePolymorphic
import kotlin.math.exp

/**
 * copy from src/Lean/Widget/InteractiveCode.lean:45
 * The polymorphism is achieved via class inheritance
 * TODO using Kotlin's property for getter/setter and avoid
 *      all fields being public
 */

interface InfoViewRenderer {
    fun toInfoViewString(sb : StringBuilder) : String
    fun contextInfo(offset: Int, startOffset: Int, endOffset : Int) : Triple<ContextInfo, Int, Int>?
}

/**
 * check src/Lean/Widget/TaggedText.lean 104 lines --16%--
 */
abstract class TaggedText<T> where T: InfoViewRenderer {
    abstract fun toInfoViewString(sb: StringBuilder, parent : TaggedText<T>?) : String

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

class TaggedTextText<T>(val text: String) : TaggedText<T>() where T: InfoViewRenderer {
    override fun toInfoViewString(sb: StringBuilder, parent : TaggedText<T>?): String {
        this.parent = parent
        startOffset = sb.length
        sb.append(text)
        endOffset = sb.length
        this.codeText = text
        return text
    }

    override fun getCodeText(offset: Int, t: T?): Triple<ContextInfo, Int, Int>? {
        return t!!.contextInfo(offset, parent!!.startOffset, parent!!.endOffset)
    }
}

class TaggedTextTag<T>(val f0: T, val f1: TaggedText<T>) : TaggedText<T>() where T: InfoViewRenderer {
    override fun toInfoViewString(sb: StringBuilder, parent : TaggedText<T>?): String {
        this.parent = parent
        // TODO handle events
        startOffset = sb.length
        f0.toInfoViewString(sb)
        f1.toInfoViewString(sb, this)
        endOffset = sb.length
        codeText = sb.substring(startOffset, endOffset)
        return codeText
    }

    override fun getCodeText(offset: Int, t: T?): Triple<ContextInfo, Int, Int>? {
        // TODO Here it must be very wrong for message
        return f1.getCodeText(offset, f0)
    }
}

class TaggedTextAppend<T>(private val append: List<TaggedText<T>>) : TaggedText<T>() where T: InfoViewRenderer {
    override fun toInfoViewString(sb: StringBuilder, parent: TaggedText<T>?) : String {
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

abstract class MsgEmbed : InfoViewRenderer

class MsgEmbedExpr(val expr: TaggedText<SubexprInfo>) : MsgEmbed() {
    override fun toInfoViewString(sb: StringBuilder): String {
        return expr.toInfoViewString(sb, null)
    }

    override fun contextInfo(offset: Int, startOffset: Int, endOffset : Int) : Triple<ContextInfo, Int, Int>? {
        return expr.getCodeText(offset, null)
    }
}

class MsgEmbedGoal(val goal: InteractiveGoal) : MsgEmbed() {
    override fun toInfoViewString(sb: StringBuilder): String {
        return goal.toInfoViewString(sb)
    }

    override fun contextInfo(offset: Int, startOffset: Int, endOffset : Int) : Triple<ContextInfo, Int, Int>? {
        // TODO when does this happen?
        TODO("Not yet implemented")
    }
}

/**
 * TODO trace! Not implement yet, check src/Lean/Widget/InteractiveDiagnostic.lean" 221 lines --11%--
 *      the definition of MsgEmbed
 */
abstract class MsgEmbedTrace(val indent: Int, val cls: String): MsgEmbed()
