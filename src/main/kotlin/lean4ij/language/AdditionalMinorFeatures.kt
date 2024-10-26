package lean4ij.language

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

/**
 * The name follows the ref:
 * https://plugins.jetbrains.com/docs/intellij/additional-minor-features.html
 * related issue: 
 */
class Lean4PairedBraceMatcher : PairedBraceMatcher {

    override fun getPairs(): Array<BracePair> {
        TODO("Not yet implemented")
    }

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int {
        TODO("Not yet implemented")
    }

}