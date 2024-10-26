package lean4ij.language

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.IncorrectOperationException
import lean4ij.language.psi.TokenType.BLOCK_COMMENT
import lean4ij.language.psi.TokenType.DOC_COMMENT
import lean4ij.language.psi.TokenType.LINE_COMMENT
import lean4ij.language.psi.TokenType.STRING

class Lean4TokenType(debugName: String) : IElementType(debugName, Lean4Language.INSTANCE)

class Lean4LexerAdapter : FlexAdapter(Lean4Lexer())

class Lean4ElementImpl(node: ASTNode) : ASTWrapperPsiElement(node)

/**
 * ref https://github.com/JetBrains/intellij-community/blob/master/platform/core-impl/src/com/intellij/openapi/fileTypes/PlainTextParserDefinition.java
 */
class Lean4ParserDefinition : ParserDefinition {

    companion object {
        val FILE = IFileElementType(Lean4Language.INSTANCE)
        val COMMENTS = TokenSet.create(LINE_COMMENT, DOC_COMMENT, BLOCK_COMMENT)
        val STRINGS = TokenSet.create(STRING)
    }

    override fun createLexer(project: Project?): Lexer = Lean4LexerAdapter()

    override fun createParser(project: Project?): PsiParser = Lean4Parser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRINGS

    override fun createElement(node: ASTNode?): PsiElement {
        // IElementType type = node.getElementType();
        // if (type == PROPERTY) {
        //     return new SimplePropertyImpl(node);
        // }
        // throw new AssertionError("Unknown element type: " + type);
        val type = node?.elementType
        if (type == FILE) {
            return Lean4ElementImpl(node)
        }
        throw AssertionError("Unknown element type: $type")
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = Lean4PsiFile(viewProvider)
}
