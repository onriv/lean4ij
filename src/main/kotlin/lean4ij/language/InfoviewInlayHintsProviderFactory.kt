package lean4ij.language

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.textmate.psi.TextMateFile

/**
 * Ref: https://github.com/JetBrains/intellij-community/blob/idea/242.21829.142/java/java-impl/src/com/intellij/codeInsight/hints/JavaImplicitTypeDeclarativeInlayHintsProvider.kt
 */
class InfoviewInlayHintsCollector : SharedBypassCollector {
    override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
        if (element is TextMateFile) {
            return
        }
        sink.addPresentation(InlineInlayPosition(10, true), hasBackground = true) {
            text("todo todo todo\nyet another line?")
        }
    }
}

class Lean4PlaceTypeDeclarativeInlayHintsProvider : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector {
        return InfoviewInlayHintsCollector()
    }
}