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

    abstract fun toInfoViewString(startOffset: Int, parent : CodeWithInfos?) : String

    abstract fun getCodeText(offset: Int) : CodeWithInfos?

}