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

/**
 * check src/Lean/Widget/TaggedText.lean" 104 lines --16%--
 */
open class TaggedText<T> {

}

class TaggedTextText<T>(val  text: String) : TaggedText<T>()

class TaggedTextTag<T>(val f0: T, val f1: TaggedText<T>) : TaggedText<T>()

class TaggedTextAppend<T>(private val append: List<TaggedText<T>>) : TaggedText<T>() {
}

open class MsgEmbed

class MsgEmbedExpr(val expr: CodeWithInfos) : MsgEmbed()
class MsgEmbedGoal(val goal: InteractiveGoal) : MsgEmbed()

/**
 * TODO trace! Not implement yet, check src/Lean/Widget/InteractiveDiagnostic.lean" 221 lines --11%--
 *      the definition of MsgEmbed
 */
class MsgEmbedTrace(val indent: Int, val cls: String): MsgEmbed()

class TaggedTextMsgEmbed : TaggedText<MsgEmbed>()