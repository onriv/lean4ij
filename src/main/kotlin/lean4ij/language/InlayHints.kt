package lean4ij.language

import com.google.common.base.MoreObjects
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.codeInsight.hints.declarative.EndOfLinePosition
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.codeInsight.hints.declarative.impl.DeclarativeInlayHintsPassFactory
import com.intellij.lang.ASTNode
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.documentation.QuickDocSyntaxHighlightingHandler
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.impl.event.MarkupModelListener
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.markup.UnmodifiableTextAttributes
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.createLifetime
import com.intellij.openapi.rd.createNestedDisposable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.util.io.await
import com.jetbrains.rd.util.lifetime.intersect
import kotlinx.coroutines.launch
import lean4ij.Lean4Settings
import lean4ij.lsp.data.InfoviewRender
import lean4ij.lsp.data.InteractiveGoalsParams
import lean4ij.lsp.data.InteractiveTermGoalParams
import lean4ij.lsp.data.PlainGoalParams
import lean4ij.lsp.data.Position
import lean4ij.project.LeanFile
import lean4ij.project.LeanProjectService
import lean4ij.util.LspUtil
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.jetbrains.plugins.textmate.psi.TextMateFile
import java.awt.Color
import java.awt.Graphics
import java.awt.Rectangle
import java.util.IdentityHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

// location is either line or absolute pos, depending on the type of hint
class Hint(val isEol: Boolean, val location: Int, val content: String)

class HintSet {
    private var hints: ArrayList<Hint> = ArrayList()

    fun add(hint: Hint) {
        this.hints.add(hint)
    }

    fun dumpHints(sink: InlayTreeSink) {
        this.hints.forEach { hint ->
            if (hint.isEol) {
                sink.addPresentation(EndOfLinePosition(hint.location), hasBackground = true) {
                    text(hint.content)

                }
            }
            else {
                hint.content.chunked(30).forEach {
                    sink.addPresentation(InlineInlayPosition(hint.location, false), hasBackground = true) {
                        text(it)
                    }
                }
            }
        }
    }
}

class HintCache {
    var cache = ConcurrentHashMap<LeanFile, Pair<Long, CompletableFuture<HintSet>>>()

    /* returns (need re-computation, displaySet) */
    fun query(file: LeanFile, time: Long): CompletableFuture<HintSet>? {
        val cur = cache[file] ?: return null

        // we do not have a run scheduled for this version
        if (cur.first != time) {
            cur.second.cancel(true)

            return null
        }
        else {
            return cur.second
        }
    }

    fun insert(file: LeanFile, time: Long, hints: CompletableFuture<HintSet>) {
        cache[file] = Pair(time, hints)
    }
}

// Base functionality
abstract class InlayHintBase(protected val editor: Editor, protected val project: Project?) : SharedBypassCollector {
    var hintCache = HintCache()

    companion object {
        const val TIMEOUT_STEP_MILLIS: Long = 25
        const val TIMEOUT_MAX_ITS = 10
    }

    override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
        if (project == null || element !is TextMateFile) {
            return
        }
        val file = MoreObjects.firstNonNull(editor.virtualFile, element.virtualFile)
        if (file.extension != "lean") {
            return
        }
        val leanProject = project.service<LeanProjectService>()
        val leanFile = leanProject.file(file);

        // if cached, return directly
        val time = element.containingFile.modificationStamp;
        val cached = hintCache.query(leanFile, time);
        val hints: CompletableFuture<HintSet>
        if (cached != null) {
            hints = cached
        }
        else {
            // recompute
            hints = CompletableFuture<HintSet>();
            hintCache.insert(leanFile, element.containingFile.modificationStamp, hints);
            leanProject.scope.launch {
                val content = editor.document.text;
                val actualHints = computeFor(leanFile, content);
                hints.complete(actualHints);
                // request rerender of hints
                // reference: https://github.com/redhat-developer/lsp4ij/blob/main/src/main/java/com/redhat/devtools/lsp4ij/internal/InlayHintsFactoryBridge.java#L59
                DeclarativeInlayHintsPassFactory.scheduleRecompute(editor, project)
                DaemonCodeAnalyzer.getInstance(project).restart(element.containingFile);
            }
        }

        // wait until future is finished
        // (see lsp4ij for reference)
        val computeTime = element.containingFile.modificationStamp;
        var its = 0
        while (!hints.isDone) {
            its += 1
            if (element.containingFile.modificationStamp != computeTime || its >= TIMEOUT_MAX_ITS) {
                return
            }

            try {
                hints.get(TIMEOUT_STEP_MILLIS, TimeUnit.MILLISECONDS);
            }
            catch (_: TimeoutException) {
                // Ignore timeout
            }
        }

        hints.get().dumpHints(sink);
    }

    abstract suspend fun computeFor(file: LeanFile, content: String): HintSet;
}

/**
 * Ref: https://github.com/JetBrains/intellij-community/blob/idea/242.21829.142/java/java-impl/src/com/intellij/codeInsight/hints/JavaImplicitTypeDeclarativeInlayHintsProvider.kt
 * This is the inlay hints for omit types like
 *     def a := 1
 * or
 *     set a := 1
 * in a proof
 */
class OmitTypeInlayHintsCollector(editor: Editor, project: Project?) : InlayHintBase(editor, project) {

    companion object {
        val DEF_REGEX = Regex("""(\b(?:def|set|let|have)\s)(.*?)(\s*:=[\n\s]+)""")
    }

    override suspend fun computeFor(file: LeanFile, content: String): HintSet {
        val hints = HintSet()
        for (m in DEF_REGEX.findAll(content)) {
            if (hasTypeNotation(m.groupValues[2])) {
                continue
            }
            val session = file.getSession()
            // This +2 is awkward, there maybe bad case for it
            val lineColumn = StringUtil.offsetToLineColumn(content, m.range.last + 2)
//            val position = Position(line = lineColumn.line, character = lineColumn.column)
            val position = Position(line = lineColumn.line, character = lineColumn.column)
            val textDocument = TextDocumentIdentifier(LspUtil.quote(file.virtualFile!!.path))
            val params = PlainGoalParams(textDocument, position)
            val interactiveTermGoalParams =
                InteractiveTermGoalParams(session, params, textDocument, position)
            // TODO what if the server not start?
            //      will it hang and leak?
            val termGoal = file.getInteractiveTermGoal(interactiveTermGoalParams) ?: continue
            val inlayHintType = ": ${termGoal.type.toInfoViewString(InfoviewRender(), null)}"
            var hintPos = m.range.last - m.groupValues[3].length ;
            // anonymous have is slightly weird
            if (m.groupValues[1] != "have " || !m.groupValues[2].isEmpty()) {
                hintPos += 1;
            }
            hints.add(Hint(false, hintPos, inlayHintType))
        }

        return hints
    }

    private fun hasTypeNotation(s: String): Boolean {
        // if there exists a balanced :, return true
        var openBracket = 0
        var openFlower = 0
        var openSquare = 0
        for (c in s) {
            if (c == '(') openBracket++
            else if (c == ')') openBracket--;
            else if (c == '{') openFlower++;
            else if (c == '}') openFlower--;
            else if (c == '[') openSquare++;
            else if (c == ']') openSquare--;
            else if (c == ':' && openBracket == 0 && openFlower == 0 && openSquare == 0) {
                return true;
            }
        }

        return false
    }
}

class OmitTypeInlayHintsProvider : InlayHintsProvider {

    companion object {
        val providers = ConcurrentHashMap<String, OmitTypeInlayHintsCollector>()
    }

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        return providers.computeIfAbsent(file.virtualFile.path) {
            OmitTypeInlayHintsCollector(editor, editor.project)
        }
    }
}

class GoalInlayHintsCollector(editor: Editor, project: Project?) : InlayHintBase(editor, project) {

    companion object {
        val lean4Settings = service<Lean4Settings>()
    }

    override suspend fun computeFor(file: LeanFile, content: String): HintSet {
        val hints = HintSet()

        for (m in lean4Settings.commentPrefixForGoalHintRegex.findAll(content)) {
            val session = file.getSession()

            val lineColumn = StringUtil.offsetToLineColumn(content, m.range.last)

            val position = Position(line = lineColumn.line, character = lineColumn.column)
            val textDocument = TextDocumentIdentifier(LspUtil.quote(file.virtualFile!!.path))

            val typeHint: String
            // assume tactic mode call
            val params = PlainGoalParams(textDocument, position)
            val interactiveGoalParams =
                InteractiveGoalsParams(session, params, textDocument, position)

            val termGoals = file.getInteractiveGoals(interactiveGoalParams)
            if (termGoals != null && !termGoals.goals.isEmpty()) {
                typeHint = termGoals.goals[0].type.toInfoViewString(InfoviewRender(), null);
            }
            else {
                // non tactic mode
                val params = PlainGoalParams(textDocument, position)
                val interactiveTermGoalParams =
                    InteractiveTermGoalParams(session, params, textDocument, position)
                val termGoal = file.getInteractiveTermGoal(interactiveTermGoalParams)
                typeHint = termGoal?.type?.toInfoViewString(InfoviewRender(), null) ?: continue
            }

            var hintPos = m.range.first + m.groupValues[1].length
            hints.add(Hint(false, hintPos, typeHint))
//            hints.add(Hint(true, lineColumn.line - 1, typeHint))
        }

        return hints
    }
}

class GoalInlayHintsProvider : InlayHintsProvider {

    companion object {
        val providers = ConcurrentHashMap<String, GoalInlayHintsCollector>()
    }

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        return providers.computeIfAbsent(file.virtualFile.path) {
            GoalInlayHintsCollector(editor, editor.project)
        }
    }
}

class PlaceHolderInlayHintsCollector(editor: Editor, project: Project?) : InlayHintBase(editor, project) {

    override suspend fun computeFor(file: LeanFile, content: String): HintSet {
        if (project == null) {
            return HintSet()
        }

        val hints = HintSet()
        val leanProject = project.service<LeanProjectService>()
        for (m in Regex("""\b_\b""").findAll(content)) {
            // TODO what if it's not start?
            // Here it's the internal language server
            val languageServer = leanProject.languageServer.await().languageServer
            val lineColumn = StringUtil.offsetToLineColumn(content, m.range.first)
            // Here we also have another lean4ij.lsp.data.Position defined and imported
            // Hence here using the full qualified name
            val position = org.eclipse.lsp4j.Position(lineColumn.line, lineColumn.column)
            val textDocument = TextDocumentIdentifier(LspUtil.quote(file.virtualFile!!.path))
            val hover = languageServer.hover(HoverParams(textDocument, position)).await()
            // TODO there are cases here that here hover is null
            if (hover != null && hover.contents.isRight) {
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
                    hints.add(Hint(false, m.range.last+1, inlayHint))
                }
            }
        }

        return hints
    }

}

class PlaceHolderInlayHintsProvider : InlayHintsProvider {
    companion object {
        val providers = ConcurrentHashMap<String, PlaceHolderInlayHintsCollector>()
    }


    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector {
        return providers.computeIfAbsent(file.virtualFile.path) {
            PlaceHolderInlayHintsCollector(editor, editor.project)
        }
    }
}


class InlayTextAttributes: UnmodifiableTextAttributes() {
    override fun getBackgroundColor(): Color? {
        return null
    }

    override fun getForegroundColor(): Color? {
        return JBColor.BLUE
    }

}

class InlayRenderer(info: HighlightInfo): HintRenderer(info.description) {
    override fun getTextAttributes(editor: Editor): TextAttributes? {
        return InlayTextAttributes()
    }

    override fun useEditorFont(): Boolean {
        return true
    }
}

// https://github.com/chylex/IntelliJ-Inspection-Lens/blob/main/src/main/kotlin/com/chylex/intellij/inspectionlens/editor/LensMarkupModelListener.kt
class DiagInlayManager(var editor: TextEditor) : MarkupModelListener {
    var currentHints: IdentityHashMap<RangeHighlighterEx, Inlay<InlayRenderer>> = IdentityHashMap()

    init {
        val model = model()

        val pluginLifetime = ApplicationManager.getApplication().service<lean4ij.services.MyProjectDisposableService>().createLifetime();
        val editorLifetime = editor.createLifetime();

        model?.addMarkupModelListener(pluginLifetime.intersect (editorLifetime).createNestedDisposable("lean4ijDiagEditorLifetime"), this)

        refresh()
    }

    fun model() : MarkupModelEx? {
        return DocumentMarkupModel.forDocument(editor.editor.document, editor.editor.project, false) as? MarkupModelEx
    }

    override fun afterAdded(highlighter: RangeHighlighterEx) {
        this.showHint(highlighter)
    }

    override fun attributesChanged(highlighter: RangeHighlighterEx, renderersChanged: Boolean, fontStyleOrColorChanged: Boolean) {
        this.updateHint(highlighter)
    }

    override fun beforeRemoved(highlighter: RangeHighlighterEx) {
        this.hideHint(highlighter)
    }

    fun hideHint(range: RangeHighlighterEx) {
        val inlay = this.currentHints[range] ?: return

        this.currentHints.remove(range)
        ApplicationManager.getApplication().invokeLater {
            inlay.dispose()
        }
    }

    fun updateHint(range: RangeHighlighterEx) {
        val info = HighlightInfo.fromRangeHighlighter(range) ?: return

        val inlay = this.currentHints[range] ?: return

        ApplicationManager.getApplication().invokeLater {
            if (info.description == null) {
                inlay.dispose()
            }
            else {
                inlay.renderer.text = info.description
            }
        }
    }

    fun showHint(range: RangeHighlighterEx) {
        val info = HighlightInfo.fromRangeHighlighter(range) ?: return

        // seems that it's always marked as weak warning
        if (info.description == null || info.severity != HighlightSeverity.WEAK_WARNING) {
            return
        }

        val renderer = InlayRenderer(info)
        val properties = InlayProperties()
            .relatesToPrecedingText(true)

        ApplicationManager.getApplication().invokeLater {
            info.endOffset
            val hint = editor.editor.inlayModel.addAfterLineEndElement(info.actualEndOffset - 1, properties, renderer) ?: return@invokeLater

            this.currentHints[range] = hint
        }
    }

    fun refresh() {

        // hide old ones
        for (hint in currentHints) {
            hint.value.dispose()
        }
        currentHints.clear()

        // create new ones
        val highlighters = model()?.allHighlighters ?: return
        for (highlighter in highlighters) {
            val highlighter = highlighter as? RangeHighlighterEx ?: continue
            this.showHint(highlighter)
        }
    }

    companion object {

        fun register(editor: TextEditor) {
            val settings = service<Lean4Settings>()
            if (settings.enableDiagnosticsLens) {
                DiagInlayManager(editor)
            }
        }
    }
}

class DiagInlayHintsCollector(editor: Editor, project: Project?) : InlayHintBase(editor, project) {

    override suspend fun computeFor(file: LeanFile, content: String): HintSet {
        if (project == null) {
            return HintSet()
        }

        // reference https://github.com/JetBrains/intellij-community/blob/b4e6a7dc1c9adb1a0b622a75ebaec5a3d4a3475e/platform/lang-impl/src/com/intellij/codeInsight/daemon/impl/TrafficLightRenderer.java#L209


        val hints = HintSet()
        val leanProject = project.service<LeanProjectService>()

        return hints
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
