package lean4ij.language

import com.google.common.io.Resources
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lexer.Lexer
import com.intellij.openapi.components.service
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
import com.intellij.psi.util.elementType
import lean4ij.setting.Lean4Settings
import lean4ij.language.psi.TokenType
import lean4ij.language.psi.TokenType.WHITE_SPACE
import java.nio.charset.StandardCharsets


/**
 * TODO use customized textAttributes
 */
class Lean4SyntaxHighlighter : SyntaxHighlighterBase() {
    val SEPARATOR: TextAttributesKey = createTextAttributesKey("LEAN_SEPARATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val KEY: TextAttributesKey = createTextAttributesKey("LEAN_KEY", DefaultLanguageHighlighterColors.KEYWORD)
    val VALUE: TextAttributesKey = createTextAttributesKey("LEAN_VALUE", DefaultLanguageHighlighterColors.STRING)
    val COMMENT: TextAttributesKey = createTextAttributesKey("LEAN_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    val NUMBER: TextAttributesKey = createTextAttributesKey("LEAN_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    val SORRY : TextAttributesKey = createTextAttributesKey("LEAN_SORRY", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)
    val KEYWORD_IN_PROOF : TextAttributesKey = createTextAttributesKey("LEAN_KEYWORD_IN_PROOF", DefaultLanguageHighlighterColors.KEYWORD)

    val BAD_CHAR_KEYS: Array<TextAttributesKey> = arrayOf(BAD_CHARACTER)
    val SEPARATOR_KEYS: Array<TextAttributesKey> = arrayOf(SEPARATOR)
    val KEY_KEYS: Array<TextAttributesKey> = arrayOf(KEY)
    val VALUE_KEYS: Array<TextAttributesKey> = arrayOf(VALUE)
    val COMMENT_KEYS: Array<TextAttributesKey> = arrayOf(COMMENT)
    val NUMBER_KEYS: Array<TextAttributesKey> = arrayOf(NUMBER)
    val EMPTY_KEYS: Array<TextAttributesKey> = arrayOf()
    val SORRY_KEYS: Array<TextAttributesKey> = arrayOf(SORRY)
    val KEYWORD_IN_PROOF_KEYS: Array<TextAttributesKey> = arrayOf(KEYWORD_IN_PROOF)


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
        if (tokenType == TokenType.KEYWORD_COMMAND6) {
            return KEYWORD_IN_PROOF_KEYS
        }
        if (tokenType == TokenType.LINE_COMMENT || tokenType == TokenType.BLOCK_COMMENT || tokenType == TokenType.DOC_COMMENT) {
            return COMMENT_KEYS;
        }
        if (tokenType == TokenType.NUMBER||tokenType==TokenType.NEGATIVE_NUMBER) {
            return NUMBER_KEYS;
        }
        if (tokenType == TokenType.KEYWORD_SORRY) {
            return SORRY_KEYS;
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
    private val lean4Settings = service<Lean4Settings>()

    companion object {
        val tactics = getAllTactics()

        private fun getAllTactics(): Map<String, String> {
            val tactics = mutableMapOf<String, String>()
            val resource = javaClass.classLoader.getResource("tactics.txt")?:return emptyMap()
            for (line in Resources.readLines(resource, StandardCharsets.UTF_8)) {
                if (line.startsWith("--")) {
                    continue
                }
                val (key, value) = line.split(" ")
                tactics[key] = value
            }
            return tactics.toMap()
        }
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.parent is Lean4Definition) {
            if (!lean4Settings.enableHeuristicDefinition) return
            if (element.node.elementType == TokenType.IDENTIFIER || element.node.elementType == TokenType.DOT) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element.textRange).textAttributes(DefaultLanguageHighlighterColors.FUNCTION_DECLARATION).create();
            }
        } else if (element.parent is Lean4Attributes) {
            if (!lean4Settings.enableHeuristicAttributes) return
            // check the parent rather than the element itself for skipping comments
            if (element.node.elementType == TokenType.IDENTIFIER) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element.textRange).textAttributes(DefaultLanguageHighlighterColors.METADATA).create();
            }
            if (element.node.elementType == TokenType.ATTRIBUTE) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element.textRange).textAttributes(DefaultLanguageHighlighterColors.KEYWORD).create();
            }
        } else if (element.node.elementType == TokenType.IDENTIFIER) {
            if (isField(element)) {
                if (!lean4Settings.enableHeuristicField) return
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element.textRange).textAttributes(DefaultLanguageHighlighterColors.INSTANCE_FIELD).create();
            } else {
                if (!lean4Settings.enableHeuristicTactic) return
                if (tactics.containsKey(element.text)) {
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(element.textRange).textAttributes(DefaultLanguageHighlighterColors.FUNCTION_CALL).create();
                }
            }
        }
    }

    private fun isField(element: PsiElement): Boolean {
        // quite loose check
        return prevSiblingIsNewLine(element) /*&& nextSiblingIsAssign(element)*/
    }

    private fun prevSiblingIsNewLine(element: PsiElement): Boolean {
        val prevElement = element.prevSibling?:return false
        return prevElement.elementType == WHITE_SPACE && prevElement.text.contains('\n')
    }

    private fun nextSiblingIsAssign(element: PsiElement): Boolean {
        var nextValidElement : PsiElement? = element.nextSibling
        while (!isValid(nextValidElement)) {
            nextValidElement = nextValidElement?.nextSibling
        }
        val elementType = nextValidElement?.node?.elementType
        return elementType == TokenType.ASSIGN || elementType == TokenType.COLON
    }

    private fun isValid(element: PsiElement?): Boolean {
        return element?.node?.elementType != WHITE_SPACE && element?.node?.elementType != TokenType.PLACEHOLDER;
    }
}
