package lean4ij.language

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.io.await
import kotlinx.coroutines.runBlocking
import lean4ij.language.OmitTypeInlayHintsCollector.Companion.DEF_REGEX
import lean4ij.lsp.data.InteractiveTermGoalParams
import lean4ij.lsp.data.PlainGoalParams
import lean4ij.lsp.data.Position
import lean4ij.project.LeanFile
import lean4ij.project.LeanProjectService
import lean4ij.util.LspUtil
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.jetbrains.plugins.textmate.psi.TextMateFile

/**
 * Ref: https://github.com/JetBrains/intellij-community/blob/idea/242.21829.142/java/java-impl/src/com/intellij/codeInsight/hints/JavaImplicitTypeDeclarativeInlayHintsProvider.kt
 * This is the inlay hints for omit types like
 *     def a := 1
 * or
 *     set a := 1
 * in a proof
 */
class OmitTypeInlayHintsCollector(private val psiFile: PsiFile, private val editor: Editor) : SharedBypassCollector {

    companion object {
        /**
         * It's very awkward doing this with regex pattern for this...
         * But we don't have a parser for lean currently
         */
        val DEF_REGEX = Regex("""(\b(?:def|set)\s+)(.+)\s+(:=[\n\s]+)""")
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
                if (hasTypeNotation(m.groupValues[2], m.range.start+m.groupValues[1].length, leanFile)) {
                    continue
                }
                val session = leanFile.getSession()
                // This +2 is awkward, there maybe bad case for it
                val lineColumn = StringUtil.offsetToLineColumn(element.text, m.range.last+2)
                val position = Position(line = lineColumn.line, character = lineColumn.column)
                val textDocument = TextDocumentIdentifier(LspUtil.quote(file.path))
                val params = PlainGoalParams(textDocument, position)
                val interactiveTermGoalParams = InteractiveTermGoalParams(session, params, textDocument, position)
                // TODO what if the server not start?
                //      will it hang and leak?
                val termGoal = leanFile.getInteractiveTermGoal(interactiveTermGoalParams) ?: continue
                val inlayHintType = ": ${termGoal.type.toInfoViewString(StringBuilder(), null)}"
                // TODO Here it's kind of awkward:
                //      com.intellij.codeInsight.hints.declarative.impl.PresentationTreeBuilderImpl.text
                //      limit the length of inlay hints to 30 characters
                inlayHintType.chunked(30).forEach {
                    // TODO what is relatedToPrevious for?
                    sink.addPresentation(InlineInlayPosition(m.range.last-m.groupValues[3].length , false), hasBackground = true) {
                        text(it)
                    }
                }
            }
        }
    }

    /**
     * A poor and wrong way to check it... since we don't have an ast parser
     * The parser for this part should not be hard though
     */
    private fun hasTypeNotation(s: String, offset: Int, leanFile: LeanFile): Boolean {
        if (!s.contains(':')) {
            return false
        }
        if (!s.substringAfterLast(':').contains(')')) {
            return true
        }
        if (!s.substringBefore(':').contains('(')) {
            return true
        }
        return false
    }
}

class OmitTypeInlayHintsProvider : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector {
        return OmitTypeInlayHintsCollector(file, editor)
    }
}

class PlaceHolderInlayHintsCollector(private val psiFile: PsiFile, private val editor: Editor) : SharedBypassCollector {
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
            for (m in Regex("""\b_\b""").findAll(element.text)) {
                // TODO what if it's not start?
                // Here it's the internal language server
                val languageServer = leanProject.languageServer.await().languageServer
                val lineColumn = StringUtil.offsetToLineColumn(element.text, m.range.first)
                // Here we also have another lean4ij.lsp.data.Position defined and imported
                // Hence here using the full qualified name
                val position = org.eclipse.lsp4j.Position(lineColumn.line, lineColumn.column)
                val textDocument = TextDocumentIdentifier(LspUtil.quote(file.path))
                val hover = languageServer.hover(HoverParams(textDocument, position)).await()
                if (hover.contents.isRight) {
                    val value = hover.contents.right.value
                    if (value.contains("placeholder")) {
                        // TODO assume that only one line..., this maybe wrong
                        val split = value.split("\n")
                        if (split.size < 2) {
                            // it seems to be something like
                            // A placeholder term, to be synthesized by unification.
                            continue
                        }
                        val inlayHint = split[1]
                        // TODO Here it's kind of awkward:
                        //      com.intellij.codeInsight.hints.declarative.impl.PresentationTreeBuilderImpl.text
                        //      limit the length of inlay hints to 30 characters
                        inlayHint.chunked(30).forEach {
                            // TODO what is relatedToPrevious for?
                            sink.addPresentation(InlineInlayPosition(m.range.last+1, false), hasBackground = true) {
                                text(it)
                            }
                        }
                    }
                }
            }
        }
    }

}

class PlaceHolderInlayHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector {
        return PlaceHolderInlayHintsCollector(file, editor)
    }

}


/**
 * TODO what's DumbAware for?
 * A try for using custom folding
 * see: https://plugins.jetbrains.com/docs/intellij/folding-builder.html#define-a-folding-builder
 * TODO custom folding seems not good enough as inlay hints and hence it's commented out in plugin.xml
 */
class PlaceholderFolding : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val placeholders = Regex("""\b_\b""").findAll(document.text)
        for (m in placeholders) {
            println(m)
        }
        return emptyArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        TODO("Not yet implemented")
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return true
    }

}