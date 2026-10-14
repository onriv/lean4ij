package lean4ij.lsp

import com.intellij.openapi.util.TextRange
import junit.framework.TestCase

class LeanLSPDiagnosticFeatureTest : TestCase() {

    fun testRangeInsideFileIsKept() {
        assertEquals(TextRange(10, 20), LeanLSPDiagnosticFeature.clampRange(TextRange(10, 20), 100))
        assertEquals(TextRange(90, 100), LeanLSPDiagnosticFeature.clampRange(TextRange(90, 100), 100))
    }

    fun testRangeEndBeyondFileIsCut() {
        // document is one character longer than the not-yet-committed PSI file
        assertEquals(TextRange(5096, 5096), LeanLSPDiagnosticFeature.clampRange(TextRange(5096, 5097), 5096))
        assertEquals(TextRange(5090, 5096), LeanLSPDiagnosticFeature.clampRange(TextRange(5090, 5100), 5096))
    }

    fun testRangeStartBeyondFileIsDropped() {
        assertNull(LeanLSPDiagnosticFeature.clampRange(TextRange(5097, 5098), 5096))
    }

    fun testEmptyRangeAtEndOfFileIsKept() {
        assertEquals(TextRange(5096, 5096), LeanLSPDiagnosticFeature.clampRange(TextRange(5096, 5096), 5096))
    }
}
