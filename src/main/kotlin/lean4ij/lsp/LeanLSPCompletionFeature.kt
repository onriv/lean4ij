package lean4ij.lsp

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.components.service
import com.intellij.psi.PsiFile
import com.intellij.psi.util.startOffset
import com.redhat.devtools.lsp4ij.client.features.LSPCompletionFeature
import lean4ij.setting.Lean4Settings
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either

/**
 * Add an impl for disable lsp completion for lean
 * for sometimes it's slow...
 */
class LeanLSPCompletionFeature : LSPCompletionFeature() {
    private val lean4Settings = service<Lean4Settings>()

    override fun isEnabled(file: PsiFile): Boolean {
        return lean4Settings.enableLspCompletion
    }

    override fun createLookupElement(
        item: CompletionItem,
        context: LSPCompletionContext
    ): LookupElement? {

        // This all fixes a bug, where accepting a completion suggestion erases everything before the cursor
        //
        // The LSP does not fill out the textEdit field properly (despite receiving insertReplaceSupport during init).
        // This makes LSP4IJ try to find the current semantic token under the caret, using its text range
        // to insert the completion into.
        // However, whenever a letter is typed, the whole semantic token map is invalidated and cleared out.
        // The map doesn't get repopulated until the completion popup is closed, and the token text range retrieval
        // method instead returns a range referring to the whole file.
        // The start of this range (0) ends up as the prefix offset for the completion, so applying the completion
        // inserts it starting from offset 0 (i.e. the start of the file), erasing everything before the caret.
        //
        // When we don't get a TextEdit from the LSP, we can create our own using the IDE's PSI elements.
        // This tells LSP4IJ that we're _certain_ about what we want to replace, bypassing the bug in the logic
        // where it would try to figure this out based on the position of the caret.

        if (item.textEdit == null) {
            // The PSI element comes from the "completion file", and has a dummy suffix appended to it.
            // Its reported length is about 19 characters longer, so the TextEdit end position is set
            // until the current cursor position.
            // When working with PSI elements, it's better to use the provided methods
            // (in this case, replacing the element with a new one would be better).
            // This is simply bolted onto the existing logic, but could be reworked by handling
            // the replacement part later on too.

            // Note that .originalElement exists, but leads to the exact same issue as described above.
            // Attempting to refer to it under certain circumstances fails to find the original element,
            // instead referring to the whole file, and so the whole text range of the file.
            val psiElement = context.parameters.position
            // There is no com.intellij.psi.util.PsiTreeUtilKt.getStartOffset in version Intellij idea 2024.1
            // For the compatibility of this version, we use psiElement.textRange.startOffset
            // For version greater than or equal to 2024.2, just psiElement.startOffset should be enough
            val elementStart = psiElement.textRange.startOffset
            // Cursor offset rather than the PSI element end pos
            val elementEnd = context.parameters.offset

            // Char offset -> (line:char)
            val lineNum = psiElement.containingFile.fileDocument.getLineNumber(elementStart)
            val lineOffset = psiElement.containingFile.fileDocument.getLineStartOffset(lineNum)

            val startPos = org.eclipse.lsp4j.Position(lineNum, elementStart - lineOffset)
            val endPos = org.eclipse.lsp4j.Position(lineNum, elementEnd - lineOffset)

            val textEdit = TextEdit(
                org.eclipse.lsp4j.Range(startPos, endPos),
                item.label
            )
            item.textEdit = Either.forLeft(textEdit)
        }
        return super.createLookupElement(item, context)
    }
}