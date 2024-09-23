package lean4ij.language

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.codeInsight.hints.declarative.impl.DeclarativeInlayHintsPassFactory
import com.intellij.codeInsight.hints.declarative.impl.DeclarativeInlayRenderer
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.userData
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.io.await
import kotlinx.coroutines.launch
import lean4ij.lsp.data.InteractiveTermGoalParams
import lean4ij.lsp.data.PlainGoalParams
import lean4ij.lsp.data.Position
import lean4ij.project.LeanFile
import lean4ij.project.LeanProjectService
import lean4ij.util.LspUtil
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.jetbrains.plugins.textmate.psi.TextMateFile
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class Hint(val location: Int, val content: String)

class HintSet {
    private var hints: ArrayList<Hint> = ArrayList()

    fun add(hint: Hint) {
        this.hints.add(hint)
    }

    fun dumpHints(sink: InlayTreeSink) {
        this.hints.forEach { hint ->
            hint.content.chunked(30).forEach {
                // TODO what is relatedToPrevious for?
                sink.addPresentation(InlineInlayPosition(hint.location, false), hasBackground = true) {
                    text(it)
                }
            }
        }
    }
}

class HintCache {
    var lastValid = ConcurrentHashMap<LeanFile, HintSet>()
    var cache = ConcurrentHashMap<LeanFile, Pair<Long, CompletableFuture<HintSet>>>()

    private fun temporaryDisplay(file: LeanFile): HintSet {
//        return lastValid[file] ?: HintSet();
        return HintSet()
    }
    /* returns (need recomputation, displaySet) */
    fun query(file: LeanFile, time: Long): Pair<Boolean, HintSet> {
        val cur = cache[file] ?: return Pair(true, HintSet());

        // we do not have a run scheduled for this version
        if (cur.first != time) {
            if (cur.second.isDone) {
                lastValid[file] = cur.second.get()
            }
            else {
                // if old, cancel it (new run will be scheduled shortly)
                cur.second.cancel(true);
            }

            return Pair(true, temporaryDisplay(file));
        }
        else if (cur.second.isDone) {
            // cache hit
            lastValid[file] = cur.second.get()
            return Pair(false, cur.second.get())
        }
        else {
            // we have a run for this version
            // but it's not finished

            // don't need recomputation since that's ongoing right now
            return Pair(false, temporaryDisplay(file))
        }
    }

    fun insert(file: LeanFile, time: Long, hints: CompletableFuture<HintSet>) {
        // TODO is this condition guaranteed?
        assert(file.virtualFile != null)
        cache[file] = Pair(time, hints);
    }
}

// Base functionality
abstract class InlayHintBase(private val editor: Editor, protected val project: Project?) : SharedBypassCollector {
    var hintCache = HintCache()

    override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
        if (project == null || element !is TextMateFile) {
            return
        }
        val file = editor.virtualFile
        if (file.extension != "lean") {
            return
        }
        val leanProject = project.service<LeanProjectService>()
        val leanFile = leanProject.file(file);

        // if cached, return directly
        val time = element.containingFile.modificationStamp;
        val cached = hintCache.query(leanFile, time);
        cached.second.dumpHints(sink);

        if (cached.first) {
            // recompute
            val hints = CompletableFuture<HintSet>();
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
        /**
         * It's very awkward doing this with regex pattern for this...
         * But we don't have a parser for lean currently
         */
        val DEF_REGEX = Regex("""(\b(?:def|set|let|have)\s+)(.+)\s+(:=[\n\s]+)""")
    }

    override suspend fun computeFor(file: LeanFile, content: String): HintSet {
        val hints = HintSet()
        for (m in DEF_REGEX.findAll(content)) {
            if (hasTypeNotation(m.groupValues[2], m.range.first + m.groupValues[1].length, file)) {
                continue
            }
            val session = file.getSession()
            // This +2 is awkward, there maybe bad case for it
            val lineColumn = StringUtil.offsetToLineColumn(content, m.range.last + 2)
            val position = Position(line = lineColumn.line, character = lineColumn.column)
            val textDocument = TextDocumentIdentifier(LspUtil.quote(file.virtualFile!!.path))
            val params = PlainGoalParams(textDocument, position)
            val interactiveTermGoalParams =
                InteractiveTermGoalParams(session, params, textDocument, position)
            // TODO what if the server not start?
            //      will it hang and leak?
            val termGoal = file.getInteractiveTermGoal(interactiveTermGoalParams) ?: continue
            val inlayHintType = ": ${termGoal.type.toInfoViewString(StringBuilder(), null)}"
            hints.add(Hint(m.range.last - m.groupValues[3].length, inlayHintType))
        }

        return hints
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

    companion object {
        val providers = ConcurrentHashMap<String, OmitTypeInlayHintsCollector>()
    }

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        return providers.computeIfAbsent(file.virtualFile.path) {
            OmitTypeInlayHintsCollector(editor, editor.project)
        }
    }
}

/**
 * TODO DRY DRY DRY
 */
class PlaceHolderInlayHintsCollector(private val psiFile: PsiFile, private val editor: Editor, private val project: Project?) : SharedBypassCollector {

    private val inlayHintsCache = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.MINUTES)
        // code hash to
        .build<String, CompletableFuture<List<Pair<Int, String>>>>(
            object : CacheLoader<String, CompletableFuture<List<Pair<Int, String>>>>() {
                override fun load(key: String): CompletableFuture<List<Pair<Int, String>>> {
                    if (project == null) {
                        throw NullPointerException();
                    }
                    val ret = CompletableFuture<List<Pair<Int, String>>>()
                    val leanProject = project.service<LeanProjectService>()
                    leanProject.scope.launch {
                        val file = psiFile.virtualFile
                        val leanFile = leanProject.file(file)
                        val hints = mutableListOf<Pair<Int, String>>()
                        for (m in Regex("""\b_\b""").findAll(key)) {
                            // TODO what if it's not start?
                            // Here it's the internal language server
                            val languageServer = leanProject.languageServer.await().languageServer
                            val lineColumn = StringUtil.offsetToLineColumn(key, m.range.first)
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
                                    hints.add(Pair(m.range.last+1, inlayHint))
                                }
                            }
                        }
                        ret.complete(hints)
                    }
                    return ret
                }
            }
        )

    override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
        return;
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
        val text = editor.document.text
        val inlayHintFuture = inlayHintsCache.get(text)
        if (inlayHintFuture.isDone) {
            inlayHintFuture.get().forEach { inlayHintData ->
                // TODO Here it's kind of awkward:
                //      com.intellij.codeInsight.hints.declarative.impl.PresentationTreeBuilderImpl.text
                //      limit the length of inlay hints to 30 characters
                inlayHintData.second.chunked(30).forEach {
                    // TODO what is relatedToPrevious for?
                    sink.addPresentation(InlineInlayPosition(inlayHintData.first, false), hasBackground = true) {
                        text(it)
                    }
                }
            }

        }
    }

}

class PlaceHolderInlayHintsProvider : InlayHintsProvider {
    companion object {
        val providers = ConcurrentHashMap<String, PlaceHolderInlayHintsCollector>()
    }


    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector {
        return providers.computeIfAbsent(file.virtualFile.path) {
            PlaceHolderInlayHintsCollector(file, editor, editor.project)
        }
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