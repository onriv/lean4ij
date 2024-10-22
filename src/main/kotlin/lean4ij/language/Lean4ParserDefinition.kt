package lean4ij.language

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lexer.EmptyLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiUtilCore

class EmptyParser : PsiParser {
    /**
     * copy from [org.jetbrains.plugins.textmate.psi.TextMateParser]
     */
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val mark = builder.mark()
        while (!builder.eof()) {
            builder.advanceLexer()
        }
        mark.done(root)
        return builder.treeBuilt
    }

}

/**
 * copy from [com.intellij.openapi.fileTypes.PlainTextParserDefinition]
 */
class Lean4ParserDefinition : ParserDefinition {

    companion object {
        val LEAN4_FILE_ELEMENT_TYPE = IFileElementType(Lean4FileType.language)
    }

    override fun createLexer(project: Project?): Lexer {
        return EmptyLexer();
    }

    override fun createParser(project: Project?): PsiParser {
        return EmptyParser()
    }

    override fun getFileNodeType(): IFileElementType {
        return LEAN4_FILE_ELEMENT_TYPE
    }

    override fun getCommentTokens(): TokenSet {
        return TokenSet.EMPTY;
    }

    override fun getStringLiteralElements(): TokenSet {
        return TokenSet.EMPTY;
    }

    override fun createElement(node: ASTNode?): PsiElement {
        return PsiUtilCore.NULL_PSI_ELEMENT;
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return Lean4PsiFile(viewProvider)
    }
}
