package lean4ij.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors.BAD_CHARACTER
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import lean4ij.language.psi.TokenType
import lean4ij.language.psi.TokenType.IDENTIFIER


/**
 * TODO use customized textAttributes
 */
class Lean4SyntaxHighlighter : SyntaxHighlighterBase() {
    val SEPARATOR: TextAttributesKey = createTextAttributesKey("SIMPLE_SEPARATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val KEY: TextAttributesKey = createTextAttributesKey("SIMPLE_KEY", DefaultLanguageHighlighterColors.KEYWORD)
    val VALUE: TextAttributesKey = createTextAttributesKey("SIMPLE_VALUE", DefaultLanguageHighlighterColors.STRING)
    val COMMENT: TextAttributesKey = createTextAttributesKey("SIMPLE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    val NUMBER: TextAttributesKey = createTextAttributesKey("SIMPLE_NUMBER", DefaultLanguageHighlighterColors.NUMBER)

    val BAD_CHAR_KEYS: Array<TextAttributesKey> = arrayOf(BAD_CHARACTER)
    val SEPARATOR_KEYS: Array<TextAttributesKey> = arrayOf(SEPARATOR)
    val KEY_KEYS: Array<TextAttributesKey> = arrayOf(KEY)
    val VALUE_KEYS: Array<TextAttributesKey> = arrayOf(VALUE)
    val COMMENT_KEYS: Array<TextAttributesKey> = arrayOf(COMMENT)
    val NUMBER_KEYS: Array<TextAttributesKey> = arrayOf(NUMBER)
    val EMPTY_KEYS: Array<TextAttributesKey> = arrayOf()


    override fun getHighlightingLexer(): Lexer {
        return Lean4LexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        if (tokenType == null) {
            return emptyArray()
        }
        if (tokenType == TokenType.KEYWORD_COMMAND1 ||
            tokenType == TokenType.KEYWORD_COMMAND2 ||
            tokenType == TokenType.KEYWORD_COMMAND3 ||
            tokenType == TokenType.KEYWORD_COMMAND4 ||
            tokenType == TokenType.KEYWORD_COMMAND5 ||
            tokenType == TokenType.KEYWORD_MODIFIER ||
            tokenType == TokenType.DEFAULT_TYPE ||
            tokenType == TokenType.KEYWORD_COMMAND_PREFIX
            ) {
            return KEY_KEYS;
        }
        if (tokenType == TokenType.LINE_COMMENT || tokenType == TokenType.BLOCK_COMMENT || tokenType == TokenType.DOC_COMMENT) {
            return COMMENT_KEYS;
        }
        if (tokenType == TokenType.NUMBER||tokenType==TokenType.NEGATIVE_NUMBER) {
            return NUMBER_KEYS;
        }
        return EMPTY_KEYS;
    }
}

class Lean4SyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return Lean4SyntaxHighlighter()
    }
}

/**
 * ref: https://plugins.jetbrains.com/docs/intellij/syntax-highlighting-and-error-highlighting.html
 * TODO use customized text attributes
 */
class Lean4Annotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.parent is Lean4Definition) {
            if (element.node.elementType == IDENTIFIER) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element.textRange).textAttributes(DefaultLanguageHighlighterColors.FUNCTION_DECLARATION).create();
            }
        }
    }
}
