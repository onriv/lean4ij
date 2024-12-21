package lean4ij.project

import com.google.gson.JsonElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.DefaultLineMarkerRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.LineMarkerRendererEx
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.ProgressReporter
import com.intellij.platform.util.progress.reportProgress
import com.intellij.platform.util.progress.withProgressText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import lean4ij.setting.Lean4Settings
import lean4ij.infoview.LeanInfoViewWindowFactory
import lean4ij.infoview.external.data.ApplyEditChange
import lean4ij.lsp.data.FileProgressProcessingInfo
import lean4ij.lsp.data.GetGoToLocationParams
import lean4ij.lsp.data.InteractiveDiagnostics
import lean4ij.lsp.data.InteractiveDiagnosticsParams
import lean4ij.lsp.data.InteractiveGoals
import lean4ij.lsp.data.InteractiveGoalsParams
import lean4ij.lsp.data.InteractiveTermGoal
import lean4ij.lsp.data.InteractiveTermGoalParams
import lean4ij.lsp.data.LazyTraceChildrenToInteractiveParams
import lean4ij.lsp.data.LineRange
import lean4ij.lsp.data.LineRangeParam
import lean4ij.lsp.data.MsgEmbed
import lean4ij.lsp.data.PlainGoalParams
import lean4ij.lsp.data.Position
import lean4ij.lsp.data.RpcCallParams
import lean4ij.lsp.data.RpcCallParamsRaw
import lean4ij.lsp.data.RpcConnectParams
import lean4ij.lsp.data.RpcKeepAliveParams
import lean4ij.lsp.data.TaggedText
import lean4ij.lsp.data.DefinitionTarget
import lean4ij.util.Constants
import lean4ij.util.LspUtil
import lean4ij.util.step
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.min


class LeanFile(private val leanProjectService: LeanProjectService, private val file: String) {

    private val lean4Settings = service<Lean4Settings>()

    companion object {
        val progressingLineMarker = DefaultLineMarkerRenderer(
            TextAttributesKey.createTextAttributesKey("LINE_PARTIAL_COVERAGE"), 8, 0, LineMarkerRendererEx.Position.LEFT
        )
    }

    /**
     * TODO this should be better named
     */
    private val unquotedFile = LspUtil.unquote(file)

    var virtualFile : VirtualFile? = null

    private val processingInfoChannel = Channel<FileProgressProcessingInfo>()
    private val project = leanProjectService.project
    private val buildWindowService: BuildWindowService = project.service()
    private val scope = leanProjectService.scope
    private val scopeIO = CoroutineScope(Dispatchers.IO)
    // private val customScope = CoroutineScope(Executors.newFixedThreadPool(10, object : ThreadFactory {
    //     private val counter = AtomicInteger(0)
    //     override fun newThread(r: Runnable): Thread {
    //         val thread = Thread()
    //         thread.name = "Lean Plugin Thread ${counter.getAndIncrement()}"
    //         return thread
    //     }
    // }).asCoroutineDispatcher())

    init {
        scope.launch {
            // TODO is it here also blocking a thread?
            // TODO add a setting for this
            while (true) {
                var info = processingInfoChannel.receive()
                var highlighters = mutableListOf<RangeHighlighter>()
                highlighters = tryAddLineMarker(info, highlighters)
                if (info.isFinished()) {
                    continue
                }
                buildWindowService.startBuild(file)
                try {
                    withBackgroundFileProgress { reporter ->
                        var currentStep = 0
                        do {
                            val newStep = info.workSize()
                            // TODO they are chance that it's negative for file progress again
                            //      this is because that, while progressing, editing it again in earlier position will
                            //      trigger file processing again
                            if (newStep >= currentStep) {
                                reporter.step(newStep - currentStep)
                                currentStep = newStep
                            }
                            info = processingInfoChannel.receive()
                            highlighters = tryAddLineMarker(info, highlighters)
                        } while (info.isProcessing())
                    }
                } catch (e: CancellationException) {

                } catch (e: Exception) {
                    // TODO here should only handle for task cancelling
                    e.printStackTrace()
                }
                buildWindowService.endBuild(file)
            }
        }
        // it seems facing some initialization order problem
        // scope.launch {
        //     getAllMessages()
        // }
    }

    /**
     * this is for avoiding flashing, a highlighter is always added in the first line
     */
    private var firstLineHighlighter :RangeHighlighter? = null
    private val leanFileProgressEmptyTextAttributesKey = TextAttributesKey.createTextAttributesKey("LEAN_FILE_PROGRESS_EMPTY")

    /**
     */
    private fun tryAddLineMarker(info: FileProgressProcessingInfo, highlighters: MutableList<RangeHighlighter>): MutableList<RangeHighlighter> {
        val ret = mutableListOf<RangeHighlighter>()
        if (!lean4Settings.enableFileProgressBar) return ret
        FileEditorManager.getInstance(project).selectedTextEditor?.let { editor ->
            if (editor.virtualFile.path == unquotedFile) {
                val document = editor.document
                val markupModel = editor.markupModel
                if (firstLineHighlighter == null) {
                    firstLineHighlighter = markupModel.addLineHighlighter(0, 1, null)
                }
                firstLineHighlighter!!.lineMarkerRenderer = leanFileProgressFinishedFillingLineMarkerRender
                for (highlighter in highlighters) {
                    markupModel.removeHighlighter(highlighter)
                }
                for (processingInfo in info.processing) {
                    val startLine = processingInfo.range.start.line.let {
                        if (it == 0) {
                            firstLineHighlighter!!.lineMarkerRenderer = leanFileProgressFillingLineMarkerRender
                            1
                        } else {
                            it
                        }
                    }
                    val endLine = min(processingInfo.range.end.line, document.lineCount)
                    val startLineOffset = StringUtil.lineColToOffset(document.charsSequence, startLine, 0)
                    val endLineOffset = StringUtil.lineColToOffset(document.charsSequence, min(endLine, document.lineCount-1), 0)
                    val rangeHighlighter = markupModel.addRangeHighlighter(
                        leanFileProgressEmptyTextAttributesKey,
                        startLineOffset, endLineOffset, HighlighterLayer.LAST, HighlighterTargetArea.LINES_IN_RANGE)
                    rangeHighlighter.lineMarkerRenderer = leanFileProgressFillingLineMarkerRender
                    ret.add(rangeHighlighter)
                }
            }
        }
        return ret
    }

    private suspend fun withBackgroundFileProgress(action: suspend (reporter: ProgressReporter) -> Unit) {
        withBackgroundProgress(project, Constants.FILE_PROGRESS) {
            withProgressText(leanProjectService.getRelativePath(file)) {
                reportProgress { reporter ->
                    action(reporter)
                }
            }
        }
    }

    /**
     * current file update caret
     * now it's just forward back to project service
     * but maybe later it can do its customized job
     * TODO NOW it's very awkward also to add getAllMessages for it seems
     *      different time point: updating goals and term goal is at caret moving
     *      but all message is updating at after diagnostic finished, maybe just
     *      get the content here...
     * TODO the keypoint maybe currently no tree structure for infoview like html that can be rendered
     *      more smoothly and independently
     * TODO maybe try psi for infoview tool window
     * TODO passing things like editor etc seems cumbersome, maybe add some implement for context
     * TODO this should maybe named as [updateInternalInfoview], but it contains a switch...
     *      The switch should put in [updateInternalInfoview]
     * TODO maybe move some logic back to [lean4ij.project.listeners.LeanFileCaretListener]
     *      here in fact messed up two different source for update infoview : from the caret change and from the document update
     */
    fun updateCaret(editor: Editor, logicalPosition: LogicalPosition, forceUpdate: Boolean = false) {
        val position = Position(line = logicalPosition.line, character = logicalPosition.column)
        val textDocument = TextDocumentIdentifier(LspUtil.quote(file))
        val params = PlainGoalParams(textDocument, position)
        if (lean4Settings.enableVscodeInfoview) {
            // TODO this is in fact not fully controlling the behavior for the vscode/internal/jcef infoview
            leanProjectService.updateCaret(params)
        }
        if (lean4Settings.enableNativeInfoview) {
            if (!lean4Settings.autoUpdateInternalInfoview && !forceUpdate) return
            updateInternalInfoview(editor, params)
        } else {
            LeanInfoViewWindowFactory.getLeanInfoview(project)?.let { leanInfoviewWindow ->
                leanProjectService.scope.launch {
                    leanInfoviewWindow.updateDirectText("Internal infoview is not enable.")
                }
            }
        }
    }

    private fun updateInternalInfoview(editor: Editor, params: PlainGoalParams) {
        val textDocument = params.textDocument
        val position = params.position
        leanProjectService.scope.launch {
            if (virtualFile == null) {
                thisLogger().info("No virtual file for $file, skip updating infoview")
                return@launch
            }
            val session = getSession()
            val interactiveGoalsParams = InteractiveGoalsParams(session, params, textDocument, position)
            val interactiveTermGoalParams = InteractiveTermGoalParams(session, params, textDocument, position)
            // TODO how to determine which diagnostic get?
            val line = position.line
            val diagnosticsParams = InteractiveDiagnosticsParams(session, LineRangeParam(LineRange(line, line+1)), textDocument, position)
            val interactiveGoalsAsync = async { getInteractiveGoals(interactiveGoalsParams) }
            val interactiveTermGoalAsync = async { getInteractiveTermGoal(interactiveTermGoalParams) }
            val interactiveDiagnosticsAsync = async { getInteractiveDiagnostics(diagnosticsParams) }
            // val diagnostics = file.getInteractiveDiagnostics(diagnosticsParams)
            // Both interactiveGoals and interactiveTermGoal can be null and hence we pass them to
            // updateInteractiveGoal nullable
            val interactiveGoals = interactiveGoalsAsync.await()
            val interactiveTermGoal = interactiveTermGoalAsync.await()
            val interactiveDiagnostics = interactiveDiagnosticsAsync.await()
            // TODO the arguments are passing very deep, need some refactor
            LeanInfoViewWindowFactory.updateInteractiveGoal(editor, project, virtualFile!!, position, interactiveGoals, interactiveTermGoal, interactiveDiagnostics, allMessage)
        }
    }

    fun updateFileProcessingInfo(info: FileProgressProcessingInfo) {
        scope.launch {
            processingInfoChannel.send(info)
        }
    }

    private var session : String? = null
    private val sessionMutex : Mutex = Mutex()
    suspend fun getSession() : String {
        updateSession(null)
        return session!!
    }

    /**
     * Here the argument [oldSession] must be passed for there maybe concurrent access for updating session, for example
     * multiple rpc calls like "Lean.Widget.getInteractiveGoals" and "Lean.Widget.getInteractiveTermGoal" and
     * "Lean.Widget.getWidgets" etc
     * TODO check [Mutex]'s behavior, for example: in [here](https://discuss.kotlinlang.org/t/is-it-always-safe-to-just-convert-synchronized-to-mutex-withlock/26519)
     * TODO check if it's better way than double locking check
     */
    private suspend fun updateSession(oldSession: String?) {
        if (oldSession == session) {
            // TODO check this timeout, check the following rpcConnect for the following timeout
            withTimeout(5*1000) {
                sessionMutex.withLock {
                    if (oldSession == session) {
                        session = leanProjectService.languageServer.await().rpcConnect(RpcConnectParams(file)).sessionId
                        // keep alive making infoToInteractive behave better, for the reference must have the same session
                        // as the goal result, so keep it alive here...
                        // TODO is here will cause multiple keep alive loop?
                        keepAlive()
                    }
                }
            }
        }
    }


    /**
     * TODO maybe it should not always keep alive
     */
    private fun keepAlive() {
        scopeIO.launch {
            while (true) {
                delay(9 * 1000)
                leanProjectService.languageServer.await().rpcKeepAlive(RpcKeepAliveParams(file, session!!))
            }
        }
    }

    suspend fun getInteractiveGoals(params: InteractiveGoalsParams): InteractiveGoals? {
        return rpcCallWithRetry(params) {
            leanProjectService.languageServer.await().getInteractiveGoals(it)
        }
    }

    public suspend fun getInteractiveTermGoal(params : InteractiveTermGoalParams) : InteractiveTermGoal? {
        return rpcCallWithRetry(params) {
            leanProjectService.languageServer.await().getInteractiveTermGoal(it)
        }
    }

    public suspend fun lazyTraceChildrenToInteractive(params: LazyTraceChildrenToInteractiveParams) : List<TaggedText<MsgEmbed>>? {
        return rpcCallWithRetry(params) {
            leanProjectService.languageServer.await().lazyTraceChildrenToInteractive(it)
        }
    }

    private suspend fun getInteractiveDiagnostics(params : InteractiveDiagnosticsParams) : List<InteractiveDiagnostics>? {
        return rpcCallWithRetry(params) {
            leanProjectService.languageServer.await().getInteractiveDiagnostics(it)
        }
    }

    suspend fun getGotoLocation(params: GetGoToLocationParams) : List<DefinitionTarget>? {
         return rpcCallWithRetry(params) {
             leanProjectService.languageServer.await().getGotoLocation(it)
         }
    }

    private suspend fun <Params, Resp> rpcCallWithRetry(params: Params, action: suspend (Params) -> Resp): Resp?
            where Params: RpcCallParams {
        try {
            return action(params)
        } catch (ex: ResponseErrorException) {
            // TODO these codes are defined in org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
            //      just don't know if it's full range
            //      no! it's not full range
            // TODO refactor this
            val responseError = ex.responseError
            // TODO remove this magic number and find lean source code for it
            if (responseError.code == -32900 && responseError.message == "Outdated RPC session") {
                // Here there is a possibility that rpcCallRaw is called concurrently and all of them failed
                // the lock in updateSession will avoid update session continuously
                // also check the comment inside updateSession, in fact we keep it alive forever...
                updateSession(params.sessionId)
                params.sessionId = session!!
                return action(params)
            }
            if (responseError.code == -32603 && responseError.message == "elaboration interrupted") {
                return null
            }
            if (responseError.code == -32601 && responseError.message.contains("No RPC method")) {
                /**
                 * TODO this seems weird too
                 *      2024-08-11 14:17:38,335 [ 624441]   WARN - org.eclipse.lsp4j.jsonrpc.RemoteEndpoint - Unmatched response message: {
                 *        "jsonrpc": "2.0",
                 *        "id": "142",
                 *        "error": {
                 *          "code": -32601,
                 *          "message": "No RPC method \u0027Lean.Widget.getInteractiveDiagnostics\u0027 found"
                 *        }
                 *      }
                 */
                return null
            }
            /**
             * TODO for the following error ,
             *      Error: {
             *          "code": -32801,
             *          "message": "Cannot process request to closed file \u0027file:///....\u0027"
             *      }
             * should it be automatically reopen?
             */
            if (responseError.code == -32801 && responseError.message.contains("Cannot process request to closed file ")) {
                return null
            }
            if (responseError.code == -32602 && responseError.message.contains("Cannot decode params in RPC call")) {
                /**
                 * TODO weird for this error
                 *      handle it
                 * {
                 *   "code": -32602,
                 *   "message": "Cannot decode params in RPC call \u0027Lean.Widget.InteractiveDiagnostics.infoToInteractive({\"p\":\"2\"})\u0027\nRPC reference \u00272\u0027 is not valid"
                 * }
                 */
                return null
            }
            throw ex
        } catch (ex: Exception) {
            // org.eclipse.lsp4j.jsonrpc.ResponseErrorException: elaboration interrupted
            // TODO outdated session seems not reported here
            throw ex
        }
    }

    suspend fun rpcCallRaw(params: RpcCallParamsRaw): JsonElement? {
        // always use the session in the file rather than the external infoview
        params.sessionId = session!!
        return rpcCallWithRetry(params) {
            leanProjectService.languageServer.await().rpcCall(it)
        }
    }

    /**
     * TODO add log/notification in intellij idea for it
     */
    suspend fun restart() {
        FileEditorManager.getInstance(project).selectedTextEditor?.let { editor ->
            if (editor.virtualFile.path == unquotedFile) {
                session = null
                val languageServer = leanProjectService.languageServer.await()
                val didCloseParams = DidCloseTextDocumentParams(TextDocumentIdentifier(file))
                languageServer.didClose(didCloseParams)
                val textDocumentItem = TextDocumentItem(
                    file, Constants.LEAN_LANGUAGE_ID, 0,
                    String(editor.virtualFile.contentsToByteArray(), StandardCharsets.UTF_8)
                )
                val didOpenTextDocumentParams = DidOpenTextDocumentParams(textDocumentItem)
                languageServer.didOpen(didOpenTextDocumentParams)
            }
        }
    }

    /**
     * TODO can this be replaced with flow?
     * TODO this form is changed from a initialization order error
     *      that getAllMessages run before it init
     */
    private val diagnosticsChannel = run {
        val channel = Channel<List<Diagnostic>>()
        leanProjectService.scope.launch {
            this@LeanFile.getAllMessages(channel)
        }
        channel
    }

    private var allMessage : List<InteractiveDiagnostics>? = null

    private suspend fun getAllMessages(channel: Channel<List<Diagnostic>>) {
        var lastMaxLine = -1
        var maxLine = -1
        while (true) {
            try {
                val diagnostics = withTimeout(1 * 1000) {
                    channel.receive()
                }
                // if it's empty, trigger a getAllMessage
                if (diagnostics.isEmpty()) {
                    maxLine = lastMaxLine
                }
                for (diagnostic in diagnostics) {
                    maxLine = max(maxLine, diagnostic.range.end.line)
                }
            } catch (ex: TimeoutCancellationException) {
                if (maxLine > -1) {
                    // TODO here do get all messages
                    // TODO not sure this maxLine logic is correct or necessary
                    //      it seems it quite often just quite almost the end of the file
                    // TODO if triggered all messages, should intermediately render the infoview like invoke updateCaret?
                    thisLogger().info("get all messages for $file, maxLine: $maxLine")
                    val session = getSession()
                    val position = Position(0, 0)
                    val textDocument = TextDocumentIdentifier(LspUtil.quote(file))
                    val diagnosticsParams = InteractiveDiagnosticsParams(
                        session,
                        LineRangeParam(LineRange(0, maxLine + 1)),
                        textDocument,
                        position
                    )
                    allMessage = getInteractiveDiagnostics(diagnosticsParams)
                    // after getting all Messages, do an update intermediately...
                    // to avoid lag
                    updateCaretIntermediately()
                    lastMaxLine = maxLine
                    maxLine = -1
                }
            }
        }
    }

    private fun updateCaretIntermediately() {
        FileEditorManager.getInstance(project).selectedTextEditor?.let { editor ->
            if (editor.virtualFile.path == unquotedFile) {
                updateCaret(editor, editor.caretModel.logicalPosition)
            }
        }
    }

    /**
     * checking a bug for all messages not updated correctly. It shows that there is a cases like:
     * [Trace - 10:59:11] Received notification 'textDocument/publishDiagnostics'
     * Params: {
     *   "uri": "....",
     *   "diagnostics": [],
     *   "version": 30
     * }
     * this simply may be a notification for triggering all message, and hence we pass it to the specific file
     * even diagnostics is empty
     */
    fun publishDiagnostics(diagnostics: PublishDiagnosticsParams) {
        scope.launch {
            diagnosticsChannel.send(diagnostics.diagnostics)
        }
        for (d in diagnostics.diagnostics) {
            buildWindowService.addBuildEvent(file, d.message)
        }
    }

    fun applyEdit(changes: List<ApplyEditChange>) {
        if (virtualFile == null) {
            return
        }
        if (changes.isEmpty()) {
            return
        }
        // TODO this is kind of duplicated with tryAddLineMarker?
        //      maybe the logic for getting editor can be abstract and unify
        FileEditorManager.getInstance(project).selectedTextEditor?.let { editor ->
            if (editor.virtualFile.path == unquotedFile) {
                val document = editor.document
                var text = document.text
                changes.map { change ->
                    val start = change.range.start
                    val end = change.range.end
                    val startPos = StringUtil.lineColToOffset(text, start.line, start.character)
                    val endPos = StringUtil.lineColToOffset(text, end.line, end.character)
                    Triple(startPos, endPos, change)
                }.sortedBy { p ->
                    // sort the changes from the end to the start so that the offset does not change for replacing the range
                    -p.first
                }.forEach { p ->
                    text = text.replaceRange(p.first, p.second, p.third.newText)
                }
                val application = ApplicationManager.getApplication()
                // TODO tested and it seems
                application.invokeLater {
                    application.runWriteAction {
                        document.setText(text)
                    }
                }
            }
        }
    }

}

