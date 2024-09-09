package lean4ij.lsp.data

/**
 * copy from src/Lean/Widget/InteractiveCode.lean:45
 * The polymorphism is achieved via class inheritance
 * TODO using Kotlin's property for getter/setter and avoid
 *      all fields being public
 */
abstract class CodeWithInfos {
    @Transient
    var startOffset : Int = -1

    @Transient
    var endOffset : Int = -1

    @Transient
    var codeText : String = ""

    @Transient
    var parent : CodeWithInfos? = null

    abstract fun toInfoViewString(sb : StringBuilder, parent : CodeWithInfos?) : String

    abstract fun getCodeText(offset: Int) : CodeWithInfos?

}

interface InfoViewRenderer {
    fun toInfoViewString(sb : StringBuilder) : String
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
    abstract fun getCodeText(offset: Int) : TaggedText<T>?
}

class TaggedTextText<T>(val  text: String) : TaggedText<T>() where T: InfoViewRenderer {
    override fun toInfoViewString(sb: StringBuilder, parent : TaggedText<T>?): String {
        this.parent = parent
        startOffset = sb.length
        sb.append(text)
        endOffset = sb.length
        this.codeText = text
        return text}

    override fun getCodeText(offset: Int): TaggedText<T>? {
        return this
    }
}

class TaggedTextTag<T>(val f0: T, val f1: TaggedText<T>) : TaggedText<T>() where T: InfoViewRenderer {
    override fun toInfoViewString(sb: StringBuilder, parent : TaggedText<T>?): String {
        this.parent = parent
        // TODO handle events
        startOffset = sb.length
        f1.toInfoViewString(sb, this)
        endOffset = sb.length
        codeText = sb.substring(startOffset, endOffset)
        return codeText
    }

    override fun getCodeText(offset: Int): TaggedText<T>? {
        return f1.getCodeText(offset)
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

    override fun getCodeText(offset: Int): TaggedText<T>? {
        for (c in append) {
            if (c.startOffset <= offset && offset < c.endOffset) {
                return c.getCodeText(offset)
            }
        }
        return null
    }
}

abstract class MsgEmbed : InfoViewRenderer

class MsgEmbedExpr(val expr: CodeWithInfos) : MsgEmbed() {
    override fun toInfoViewString(sb: StringBuilder): String {
        return expr.toInfoViewString(sb, null)
    }
}

class MsgEmbedGoal(val goal: InteractiveGoal) : MsgEmbed() {
    override fun toInfoViewString(sb: StringBuilder): String {
        return goal.toInfoViewString(sb)
    }
}

/**
 * TODO trace! Not implement yet, check src/Lean/Widget/InteractiveDiagnostic.lean" 221 lines --11%--
 *      the definition of MsgEmbed
 */
abstract class MsgEmbedTrace(val indent: Int, val cls: String): MsgEmbed()

abstract class TaggedTextMsgEmbed : TaggedText<MsgEmbed>()