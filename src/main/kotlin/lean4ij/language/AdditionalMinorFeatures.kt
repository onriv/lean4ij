package lean4ij.language

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import lean4ij.language.psi.TokenType.*

/**
 * The name follows the ref:
 * https://plugins.jetbrains.com/docs/intellij/additional-minor-features.html
 * related issue:
 * TODO This seems must not use textmate highlighter
 */
class Lean4PairedBraceMatcher : PairedBraceMatcher {

    companion object {
        val PAIRS = arrayOf(
            BracePair(LEFT_BRACE, RIGHT_BRACE, false),
            BracePair(LEFT_PAREN, RIGHT_PAREN, false),
            BracePair(LEFT_BRACKET, RIGHT_BRACKET, false),
            BracePair(LEFT_UNI_BRACKET, RIGHT_UNI_BRACKET, false),
        )
    }

    override fun getPairs(): Array<BracePair> {
        return PAIRS
    }

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean {
        return true
    }

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int {
        return openingBraceOffset
    }

}