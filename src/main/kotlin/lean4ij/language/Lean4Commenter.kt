package lean4ij.language

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.generation.CommenterDataHolder
import com.intellij.codeInsight.generation.SelfManagingCommenter
import com.intellij.codeInsight.generation.SelfManagingCommenterUtil
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.lang.Commenter
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.util.text.CharArrayUtil
import lean4ij.language.psi.TokenType.BLOCK_COMMENT
import lean4ij.setting.Lean4Settings

/**
 * copied from
 * https://github.com/intellij-rust/intellij-rust/blob/master/src/main/kotlin/org/rust/ide/commenter/RsCommenter.kt
 */
data class CommentHolder(val file: PsiFile) : CommenterDataHolder() {
    // TODO should use code style
    // fun useSpaceAfterLineComment(): Boolean = CodeStyle.getLanguageSettings(file, Lean4Language.INSTANCE).LINE_COMMENT_ADD_SPACE
    fun useSpaceAfterLineComment(): Boolean = service<Lean4Settings>().state.useSpaceAfterLineComment
    fun commentAtFirstColumn() : Boolean = service<Lean4Settings>().state.commentAtFirstColumn
    fun commentEmptyLine() : Boolean = service<Lean4Settings>().state.commentEmptyLine
}
/**
 * TODO don't know if lsp4ij and the lean language server
 *      has this or not
 * ref: maybe https://github.com/intellij-rust/intellij-rust/issues/5171
 * and https://github.com/intellij-rust/intellij-rust/blob/master/src/main/kotlin/org/rust/ide/commenter/RsCommenter.kt
 * This class is copy from RsCommenter
 * and maybe
 * https://github.com/Nordgedanken/intellij-autohotkey/blob/main/src/main/kotlin/com/autohotkey/ide/commenter/AhkCommenter.kt
 * TODO as the following note, it seems some subtly here. And I am not sure what
 */
class Lean4Commenter : Commenter, CodeDocumentationAwareCommenter, SelfManagingCommenter<CommentHolder>  {
    // act like there are no doc comments, these are handled in `RsEnterInLineCommentHandler`
    override fun isDocumentationComment(element: PsiComment?) = false
    override fun getDocumentationCommentTokenType(): IElementType? = null
    override fun getDocumentationCommentLinePrefix(): String? = null
    override fun getDocumentationCommentPrefix(): String? = null
    override fun getDocumentationCommentSuffix(): String? = null

    override fun getLineCommentTokenType(): IElementType? = null
    override fun getBlockCommentTokenType(): IElementType = BLOCK_COMMENT

    override fun getLineCommentPrefix(): String = "--"

    override fun getBlockCommentPrefix(): String = "/- "
    override fun getBlockCommentSuffix(): String = " -/"

    // unused because we implement SelfManagingCommenter
    override fun getCommentedBlockCommentPrefix(): String = "-//-"
    override fun getCommentedBlockCommentSuffix(): String = "-//-"

    override fun getBlockCommentPrefix(
        selectionStart: Int,
        document: Document,
        data: CommentHolder
    ): String = blockCommentPrefix

    override fun getBlockCommentSuffix(
        selectionEnd: Int,
        document: Document,
        data: CommentHolder
    ): String = blockCommentSuffix

    override fun getBlockCommentRange(
        selectionStart: Int,
        selectionEnd: Int,
        document: Document,
        data: CommentHolder
    ): TextRange? = SelfManagingCommenterUtil.getBlockCommentRange(
        selectionStart,
        selectionEnd,
        document,
        blockCommentPrefix,
        blockCommentSuffix
    )

    override fun insertBlockComment(
        startOffset: Int,
        endOffset: Int,
        document: Document,
        data: CommentHolder?
    ): TextRange = SelfManagingCommenterUtil.insertBlockComment(
        startOffset,
        endOffset,
        document,
        blockCommentPrefix,
        blockCommentSuffix
    )

    override fun uncommentBlockComment(
        startOffset: Int,
        endOffset: Int,
        document: Document,
        data: CommentHolder?
    ) = SelfManagingCommenterUtil.uncommentBlockComment(
        startOffset,
        endOffset,
        document,
        blockCommentPrefix,
        blockCommentSuffix
    )

    override fun isLineCommented(line: Int, offset: Int, document: Document, data: CommentHolder): Boolean {
        return LINE_PREFIXES.any { CharArrayUtil.regionMatches(document.charsSequence, offset, it) }
    }

    override fun commentLine(line: Int, offset: Int, document: Document, data: CommentHolder) {
        val addSpace = data.useSpaceAfterLineComment()
        val endOffset = document.getLineEndOffset(document.getLineNumber(offset))
        val text = document.getText(TextRange(offset, endOffset))
        val newOffset = when {
            data.commentAtFirstColumn() -> offset
            text.isEmpty() -> {
                if (!data.commentEmptyLine()) return
                offset
            }
            else ->
                offset+text.indexOfFirst { it != ' ' }
        }
        document.insertString(newOffset, "--" + if (addSpace) " " else "")
    }

    override fun uncommentLine(line: Int, offset: Int, document: Document, data: CommentHolder) {
        val prefixLen = LINE_PREFIXES.find { CharArrayUtil.regionMatches(document.charsSequence, offset, it) }?.length
            ?: return
        val hasSpace = data.useSpaceAfterLineComment() &&
                CharArrayUtil.regionMatches(document.charsSequence, offset + prefixLen, " ")
        document.deleteString(offset, offset + prefixLen + if (hasSpace) 1 else 0)
    }

    override fun getCommentPrefix(line: Int, document: Document, data: CommentHolder): String = lineCommentPrefix

    override fun createBlockCommentingState(
        selectionStart: Int,
        selectionEnd: Int,
        document: Document,
        file: PsiFile
    ): CommentHolder = CommentHolder(file)

    override fun createLineCommentingState(
        startLine: Int,
        endLine: Int,
        document: Document,
        file: PsiFile
    ): CommentHolder = CommentHolder(file)

    companion object {
        private val LINE_PREFIXES: List<String> = listOf("--")
    }

}