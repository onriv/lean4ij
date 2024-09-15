package lean4ij.language

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking
import lean4ij.lsp.data.InteractiveTermGoalParams
import lean4ij.lsp.data.PlainGoalParams
import lean4ij.lsp.data.Position
import lean4ij.project.LeanProjectService
import lean4ij.util.LspUtil
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.jetbrains.plugins.textmate.psi.TextMateFile

/**
 * Ref: https://github.com/JetBrains/intellij-community/blob/idea/242.21829.142/java/java-impl/src/com/intellij/codeInsight/hints/JavaImplicitTypeDeclarativeInlayHintsProvider.kt
 */
class InfoviewInlayHintsCollector(private val psiFile: PsiFile, private val editor: Editor) : SharedBypassCollector {

    companion object {
        /**
         * It's very awkward doing this with regex pattern for this...
         * But we don't have a parser for lean currently
         */
        val DEF_REGEX = Regex("""def\s+(\w+)\s+:=""")
    }

    override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
        if (element is TextMateFile) {
            return
        }
        val project = editor.project ?: return
        val leanProject = project.service<LeanProjectService>()
        val file = editor.virtualFile
        if (file.extension != "lean") {
            return
        }
        val leanFile = leanProject.file(file)
        // Cannot add inlay hints asynchronously... even with
        // ApplicationManager.getApplication().invokeLater { ... }
        // it will not work
        // hence it's using runBlocking
        // TODO check if this block EDT or UI
        runBlocking {
            for (m in DEF_REGEX.findAll(element.text)) {
                val session = leanFile.getSession()
                // This +2 is awkward, there maybe bad case for it
                val lineColumn = StringUtil.offsetToLineColumn(element.text, m.range.last+2)
                val position = Position(line = lineColumn.line, character = lineColumn.column)
                val textDocument = TextDocumentIdentifier(LspUtil.quote(file.path))
                val params = PlainGoalParams(textDocument, position)
                val interactiveTermGoalParams = InteractiveTermGoalParams(session, params, textDocument, position)
                val termGoal = leanFile.getInteractiveTermGoal(interactiveTermGoalParams) ?: continue
                val inlayHintType = ": ${termGoal.type.toInfoViewString(StringBuilder(), null)}"
                // TODO what is relatedToPrevious for?
                // TODO this minus 1 is also awkward
                sink.addPresentation(InlineInlayPosition(m.range.last-1, true), hasBackground = true) {
                    text(inlayHintType)
                }
            }
        }
    }
}

class Lean4PlaceTypeDeclarativeInlayHintsProvider : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector {
        return InfoviewInlayHintsCollector(file, editor)
    }
}