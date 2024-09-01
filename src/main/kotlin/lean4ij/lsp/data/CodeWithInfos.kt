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

interface InfoViewRenderable {
    abstract fun toInfoViewString(sb : StringBuilder) : String
}

/**
 * check src/Lean/Widget/TaggedText.lean" 104 lines --16%--
 */
abstract class TaggedText<T> where T: InfoViewRenderable {
    abstract fun toInfoViewString(interactiveInfoBuilder: StringBuilder)
}

class TaggedTextText<T>(val  text: String) : TaggedText<T>() where T: InfoViewRenderable {
    override fun toInfoViewString(interactiveInfoBuilder: StringBuilder) {
        // TODO
        println("TaggedTextText")
    }
}

class TaggedTextTag<T>(val f0: T, val f1: TaggedText<T>) : TaggedText<T>() where T: InfoViewRenderable {
    override fun toInfoViewString(sb: StringBuilder) {
        f0.toInfoViewString(sb)
    }
}

class TaggedTextAppend<T>(private val append: List<TaggedText<T>>) : TaggedText<T>() where T: InfoViewRenderable {
    override fun toInfoViewString(interactiveInfoBuilder: StringBuilder) {
        for (taggedText in append) {
            taggedText.toInfoViewString(interactiveInfoBuilder)
        }
    }
}

abstract class MsgEmbed : InfoViewRenderable

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