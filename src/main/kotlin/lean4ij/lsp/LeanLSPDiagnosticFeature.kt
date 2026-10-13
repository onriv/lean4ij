package lean4ij.lsp

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.LSPIJUtils
import com.redhat.devtools.lsp4ij.client.features.LSPDiagnosticFeature
import org.eclipse.lsp4j.Diagnostic

/**
 * Same as [LSPDiagnosticFeature.createAnnotation], but the annotation range is clamped to the PSI file that
 * is being annotated.
 *
 * LSP4IJ collects diagnostics in a non-blocking read action that does not wait for the document to be
 * committed, and converts LSP ranges against the [Document]. The [AnnotationHolder], however, validates
 * every range against the PSI file (`Range must be inside element being annotated: (0,N); but got: (N,N+1)`).
 * While the user is typing, the document is already one character longer than the (not yet committed) PSI
 * tree, so a Lean diagnostic that ends at the very end of the file (e.g. "unexpected end of input",
 * "unsolved goals" on the last line) triggers that assertion on every keystroke.
 *
 * See notes/2026-10-13-diagnostic-range-outside-psi-file.md.
 */
class LeanLSPDiagnosticFeature : LSPDiagnosticFeature() {

    override fun isEnabled(file: PsiFile): Boolean = true

    override fun createAnnotation(
        diagnostic: Diagnostic,
        document: Document,
        fixes: List<IntentionAction>,
        holder: AnnotationHolder,
    ) {
        val range = LSPIJUtils.toTextRange(diagnostic.range, document, null, true)
            ?: return // the language server reported an invalid range, ignore it (same as LSP4IJ)

        val annotatedFile = holder.currentAnnotationSession.file
        val limit = minOf(document.textLength, annotatedFile.textLength)
        val clamped = clampRange(range, limit)
            ?: return // the whole range is beyond the (stale) PSI file; a refresh is already scheduled

        val severity = getHighlightSeverity(diagnostic) ?: return
        val builder = holder
            .newAnnotation(severity, getMessage(diagnostic))
            .tooltip(getTooltip(diagnostic))
            .range(clamped)
        if (clamped.startOffset == clamped.endOffset) {
            builder.afterEndOfLine()
        }
        getProblemHighlightType(diagnostic.tags)?.let { builder.highlightType(it) }
        for (fix in fixes) {
            builder.withFix(fix)
        }
        builder.create()
    }

    companion object {
        /**
         * Returns [range] cut to `[0, limit]`, or null when nothing of it is inside.
         * A range that merely touches the limit (`start == limit`) is kept as an empty range so that an
         * end-of-file diagnostic is still shown after the last line.
         */
        fun clampRange(range: TextRange, limit: Int): TextRange? {
            if (range.startOffset > limit) return null
            if (range.endOffset <= limit) return range
            return TextRange(range.startOffset, limit)
        }
    }
}
