package lean4ij.project

import lean4ij.lsp.InternalLeanLanguageServer
import lean4ij.lsp.LeanLanguageServer
import lean4ij.lsp.data.PlainGoalParams
import lean4ij.lsp.data.RpcCallParamsRaw
import lean4ij.util.Constants
import lean4ij.util.LspUtil
import com.google.gson.JsonElement
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.LineColumn
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.redhat.devtools.lsp4ij.LanguageServerManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import lean4ij.lsp.data.DefinitionTarget
import lean4ij.setting.Lean4Settings
import lean4ij.util.LeanUtil
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage
import org.eclipse.lsp4j.services.LanguageServer
import java.awt.Color
import java.lang.AssertionError
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

@Service(Service.Level.PROJECT)
class LeanProjectService(val project: Project, val scope: CoroutineScope)  {
    // TODO the state maybe should move out to a delegated
    // This is default to false, for it maybe not creating by Intellij IDEA
    // in this case we think it's already created by the user using command line
    // tool, etc.
    var isProjectCreating: Boolean = false

    private var _languageServer = CompletableDeferred<LeanLanguageServer>()
    val languageServer : CompletableDeferred<LeanLanguageServer> get() = _languageServer
    private var _initializeResult = CompletableDeferred<InitializeResult>()
    private val initializeResult : CompletableDeferred<InitializeResult> = _initializeResult

    private val lean4Settings = service<Lean4Settings>()

    /**
     * Setting this to false rather than true, although it makes the language server does not start as the project
     * or ide opens, but it seems improving performance for avoiding peak cpu flush as the opening
     * TODO add this on readme
     * TODO maybe some settings for it
     * TODO it's back to true, inconsistent with readme
     * TODO this is not per project...
     */
    val isEnable : AtomicBoolean = AtomicBoolean(lean4Settings.languageServerStartingStrategy == "Eager")

    private val _caretEvent = MutableSharedFlow<PlainGoalParams>()
    val caretEvent: Flow<PlainGoalParams> get() = _caretEvent.asSharedFlow()
    private val _serverEvent = MutableSharedFlow<NotificationMessage>()
    val serverEvent : Flow<NotificationMessage> get() = _serverEvent.asSharedFlow()

    fun emitServerEvent(message: NotificationMessage) {
        scope.launch {
            _serverEvent.emit(message)
        }
    }

    private val leanFiles = ConcurrentHashMap<String, LeanFile>()

    fun file(file: VirtualFile): LeanFile {
        val ret = file(LspUtil.quote(file.path))
        ret.virtualFile = file
        return ret
    }

    fun file(file: String) : LeanFile {
        return leanFiles.computeIfAbsent(file) { LeanFile(this, file) }
    }

    fun setInitializedServer(languageServer: LanguageServer) {
        val result = this.languageServer.complete(LeanLanguageServer(languageServer as InternalLeanLanguageServer))
        if (!result) {
            // TODO there is still multiple event
            // throw IllegalStateException("languageServer already setup")
            thisLogger().warn("languageServer already setup")
        }
    }

    fun setInitializedResult(initializeResult: InitializeResult) {
        this.initializeResult.complete(initializeResult)
    }

    suspend fun awaitInitializedResult() : InitializeResult = initializeResult.await()

    fun getRelativePath(file: String): String {
        val unquotedFile = LspUtil.unquote(file)
        var prefix = project.basePath ?: return unquotedFile
        if (!prefix.endsWith("/")) {
            prefix += "/"
        }
        if (unquotedFile.startsWith(prefix)) {
            return unquotedFile.substring(prefix.length)
        }
        return unquotedFile
    }

    /**
     * TODO move to [LeanFile] for session lifecycle handling
     */
    suspend fun getSession(uri: String) : String = file(uri).getSession()

    fun updateCaret(params: PlainGoalParams) {
        scope.launch {
            _caretEvent.emit(params)
        }
    }

    suspend fun rpcCallRaw(params: RpcCallParamsRaw): JsonElement? {
        return file(params.textDocument.uri).rpcCallRaw(params)
    }

    fun restartLsp() {
        // TODO should this be lock?
        _initializeResult = CompletableDeferred()
        _languageServer = CompletableDeferred()
        LanguageServerManager.getInstance(project).stop(Constants.LEAN_LANGUAGE_SERVER_ID)
        LanguageServerManager.getInstance(project).start(Constants.LEAN_LANGUAGE_SERVER_ID)
    }

    fun resetServer() {
        // TODO should this be lock?
        _initializeResult = CompletableDeferred()
        _languageServer = CompletableDeferred()
    }

    /**
     * TODO [lean4ij.lsp.LeanLanguageServerProvider.setServerCommand] contains some duplicated logic for this
     */
    fun isLeanProject(): Boolean {
        return ToolchainService.expectedToolchainPath(project)
            .toFile().isFile
    }

    fun updateInfoviewFor(document: Document) {
        val file = FileDocumentManager.getInstance().getFile(document)?:return
        if (!LeanUtil.isLeanFile(file)) return
        val editor = EditorFactory.getInstance().getEditors(document).firstOrNull()?:return
        val lineCol : LineColumn = StringUtil.offsetToLineColumn(document.text, editor.caretModel.offset) ?: return
        val position = LogicalPosition(lineCol.line, lineCol.column)
        // TODO this may be duplicated with caret events some times
        //      but without this there are cases no caret events but document changed events
        //      maybe some debounce
        file(file).updateCaret(editor, position)
    }

    fun updateInfoviewFor(editor: Editor, forceUpdate: Boolean = false) {
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document)?:return
        if (!LeanUtil.isLeanFile(file)) return
        val lineCol : LineColumn = StringUtil.offsetToLineColumn(document.text, editor.caretModel.offset) ?: return
        val position = LogicalPosition(lineCol.line, lineCol.column)
        // TODO this may be duplicated with caret events some times
        //      but without this there are cases no caret events but document changed events
        //      maybe some debounce
        file(file).updateCaret(editor, position, forceUpdate)
    }

    /**
     * not sure why it's a list
     * case for two results:
     * ```
     * [
     *  {
     *    "targetUri": "file:///home/onriv/.elan/toolchains/leanprover--lean4---v4.11.0-rc2/src/lean/Init/Core.lean",
     *    "targetSelectionRange": {
     *      "start": {
     *        "line": 374,
     *        "character": 2
     *      },
     *      "end": {
     *        "line": 374,
     *        "character": 7
     *      }
     *    },
     *    "targetRange": {
     *      "start": {
     *        "line": 374,
     *        "character": 2
     *      },
     *      "end": {
     *        "line": 374,
     *        "character": 7
     *      }
     *    }
     *  },
     *  {
     *    "targetUri": "file:///home/onriv/.elan/toolchains/leanprover--lean4---v4.11.0-rc2/src/lean/Init/Core.lean",
     *    "targetSelectionRange": {
     *      "start": {
     *        "line": 1248,
     *        "character": 0
     *      },
     *      "end": {
     *        "line": 1248,
     *        "character": 8
     *      }
     *    },
     *    "targetRange": {
     *      "start": {
     *        "line": 1248,
     *        "character": 0
     *      },
     *      "end": {
     *        "line": 1249,
     *        "character": 12
     *      }
     *    }
     *  }
     * ```
     */
    fun getGoToLocation(targets: List<DefinitionTarget>) {
        targets.firstOrNull().let { target ->
            if (target == null) {
                return@let
            }
            // TODO this must be tested if it work in windows
            val file = LocalFileSystem.getInstance().findFileByNioFile(Path(URL(target.targetUri).path))?:return@let
            // TODO UTF_8 might fail for some locale, but no better way currently for it
            val content = String(file.contentsToByteArray(), StandardCharsets.UTF_8)
            // TODO also impl select? currently the caret put at the start pos
            val offset = StringUtil.lineColToOffset(
                content,
                target.targetRange.start.line,
                target.targetRange.start.character)
            project.service<LeanProjectService>().scope.launch(Dispatchers.EDT) {
                FileEditorManager.getInstance(project).openTextEditor(
                    OpenFileDescriptor(project, file, offset),
                    true
                )
            }
        }
    }

    private var hoverListener : EditorMouseMotionListener? = null
    private var hoverRangeHighlighter : RangeHighlighter? = null

    /**
     * Try to add highlight for current hover content in current selected editor
     * Since the returned type [Hover] does not contain the concrete hovering file,
     * we implement it here rather than the [LeanFile] class.
     * Although we can also match the request using the response in [LeanLanguageServerLifecycleListener]
     * we do not do it this way yet though
     * TODO MAYBE this should be refactor out to some editor related service or other stuff
     *      too many things in [LeanProjectService]
     * The catch in the following definition in fact cannot block the plugin notifying an
     * error when we are trying to remove a non-exist listener
     * check [com.intellij.openapi.editor.impl.EditorImpl.removeEditorMouseMotionListener] and
     * [com.intellij.openapi.diagnostic.Logger.assertTrue]
     * The error is notified at the point the error is created not the point it's caught
     * Removing non-existent listener may be caused by duplicated removal, we try to fix
     * this by set it to null after removal
     */
    fun highlightCurrentContent(hover: Hover?) {
        if (!service<Lean4Settings>().enableHoverHighlight) {
            return
        }

        FileEditorManager.getInstance(project).selectedTextEditor?.let { editor ->
            val document = editor.document
            val markupModel = editor.markupModel

            if (hoverListener != null) {
                try {
                    editor.removeEditorMouseMotionListener(hoverListener!!)
                    hoverListener = null
                } catch (e: Throwable) {
                    // There are cases that we remove non-exist listener
                    // Here we just ignore it
                    // Some concrete exception is:
                    //   java.lang.Throwable: Assertion failed
                    //   	at com.intellij.openapi.diagnostic.Logger.assertTrue(Logger.java:469)
                    //   	at com.intellij.openapi.diagnostic.Logger.assertTrue(Logger.java:478)
                }
            }
            if (hoverRangeHighlighter != null) {
                markupModel.removeHighlighter(hoverRangeHighlighter!!)
            }

            if (hover == null) {
                return
            }

            // TODO DRY DRY, duplicated with
            //      lean4ij.infoview.InfoviewMouseMotionListener.mouseMoved
            // TODO this should add some setting page for it too
            val attr = object : TextAttributes() {
                override fun getBackgroundColor(): Color {
                    // TODO document this
                    // TODO should scheme be cache?
                    val scheme = EditorColorsManager.getInstance().globalScheme
                    // TODO customize attr? or would backgroundColor null?
                    //      indeed here it can be null, don't know why Kotlin does not mark it as error
                    // TODO there is cases here the background of identifier under current caret is null
                    // TODO do this better in a way
                    var color = scheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR)
                    if (color != null) {
                        return color
                    }
                    color = scheme.getColor(EditorColors.CARET_COLOR)
                    if (color != null) {
                        return color
                    }
                    return scheme.defaultBackground
                }
            }
            try {
                hoverRangeHighlighter = markupModel.addRangeHighlighter(
                    StringUtil.lineColToOffset(document.charsSequence, hover.range.start.line, hover.range.start.character),
                    StringUtil.lineColToOffset(document.charsSequence, hover.range.end.line, hover.range.end.character),
                    HighlighterLayer.LAST,
                    attr,
                    HighlighterTargetArea.EXACT_RANGE
                )
                hoverListener = object : EditorMouseMotionListener {
                    override fun mouseMoved(e: EditorMouseEvent) {
                        if (!e.isOverText) {
                            editor.markupModel.removeHighlighter(hoverRangeHighlighter!!)
                        }
                    }
                }
                editor.addEditorMouseMotionListener(hoverListener!!)
            } catch (e: AssertionError) {
                // There may be the following exception, it may be caused by concurrently changing the content in the editor and the hover event is triggered
                // TODO Nevertheless when editing the content the hover event should not be triggered theoretically, throttled the hover event when editing rather than
                //      simply catching the exception
                /**
                 * java.lang.AssertionError: 446 (class com.intellij.openapi.editor.impl.RangeHighlighterTree); this: 902(class com.intellij.openapi.editor.impl.RangeHighlighterTree)
                 * 	at com.intellij.openapi.editor.impl.IntervalTreeImpl.checkBelongsToTheTree(IntervalTreeImpl.java:938)
                 * 	at com.intellij.openapi.editor.impl.IntervalTreeImpl.removeInterval(IntervalTreeImpl.java:969)
                 * 	at com.intellij.openapi.editor.impl.MarkupModelImpl.removeHighlighter(MarkupModelImpl.java:200)
                 * 	at lean4ij.project.LeanProjectService$highlightCurrentContent$1$1.mouseMoved(LeanProjectService.kt:337)
                 * 	at com.intellij.openapi.editor.impl.EditorImpl$MyMouseMotionListener.mouseMoved(EditorImpl.java:4857)
                 */
            }
        }
    }

    /**
     * A naive check on if current project is depending on mathlib
     * TODO definitely it requires some refactor
     */
    fun isDependingOnMathlib() : Boolean {
        val projectBasePath = project.basePath?:return false
        // TODO maybe some constant for the configuration file
        val projectConfigurationFile = Path(projectBasePath, "lakefile.lean")
        if (projectConfigurationFile.exists() && projectConfigurationFile.isRegularFile()){
            val configurationText = projectConfigurationFile.toFile().readText()
            // require mathlib and the concrete git path can be in different lines
            // check: https://grep.app/search?f.lang=Lean&f.path.pattern=lakefile.lean&regexp=true&q=require+mathlib%5B%5Cs%5CS%5D%2Bleanprover-community%2Fmathlib4
            // This definitely has some bad case, but it should enough for most case
            val mathlibDependenceRegex = Regex("require mathlib[\\s\\S]+leanprover-community/mathlib4")
            if (mathlibDependenceRegex.find(configurationText) != null) {
                return true
            }
        }
        val projectConfigurationFileToml =Path(projectBasePath, "lakefile.toml")
        if (projectConfigurationFileToml.exists() && projectConfigurationFileToml.isRegularFile()) {
            val configurationText = projectConfigurationFileToml.toFile().readText()
            // TODO absolutely we should use some toml library for this
            // This is checked with
            // https://grep.app/search?f.lang=TOML&f.path.pattern=lakefile.toml&regexp=true&q=name%5Cs*%3D%5Cs*%22mathlib%22%5B%5Cs%5CS%2B%5Dgit%5Cs*%3D%5Cs*%22https%3A%2F%2Fgithub.com%2Fleanprover-community%2Fmathlib4.git%22
            // val mathlibDependenceRegex = Regex("name\\s*=\\s*\"mathlib\"[\\s\\S+]git\\s*=\\s*\"https://github.com/leanprover-community/mathlib4.git\"")
            // The above is too much, we check only `name = mathlib` for the configuration since the following
            //     name\s*=\s*"mathlib"[\s\S]+scope = "leanprover-community"
            // i.e., the following raw string is also observed, which seems default generated by `lake new <SomeProject> math.toml`
            //     [[require]]
            //      name = "mathlib"
            //      scope = "leanprover-community"
            //      version = "git#master"
            // check https://grep.app/search?f.lang=TOML&f.path.pattern=lakefile.toml&regexp=true&q=name%5Cs*%3D%5Cs*%22mathlib%22%5B%5Cs%5CS%5D%2Bscope+%3D+%22leanprover-community%22
            val mathlibDependenceRegex = Regex("name\\s*=\\s*\"mathlib\"")
            if (mathlibDependenceRegex.find(configurationText) != null) {
                return true
            }
        }
        return false
    }
}
