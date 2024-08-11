package com.github.onriv.ijpluginlean.project

import com.github.onriv.ijpluginlean.lsp.data.*
import com.github.onriv.ijpluginlean.lsp.data.Position
import com.github.onriv.ijpluginlean.util.Constants
import com.github.onriv.ijpluginlean.util.LspUtil
import com.github.onriv.ijpluginlean.util.step
import com.google.gson.JsonElement
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.DefaultLineMarkerRenderer
import com.intellij.openapi.editor.markup.FillingLineMarkerRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.LineMarkerRendererEx
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.ProgressReporter
import com.intellij.platform.util.progress.reportProgress
import com.intellij.platform.util.progress.withProgressText
import com.intellij.util.DocumentUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import java.nio.charset.StandardCharsets

object leanFileProgressFillingLineMarkerRender : FillingLineMarkerRenderer {
    override fun getPosition(): LineMarkerRendererEx.Position {
        // TODO right seems not working?
        return LineMarkerRendererEx.Position.LEFT
    }

    /**
     * TODO the reason for keys is that it's related to theme
     *      use our own key here
     */
    // private val textAttributesKey = TextAttributesKey.createTextAttributesKey("LEAN_FILE_PROGRESS")
    private val textAttributesKey = TextAttributesKey.createTextAttributesKey("LINE_PARTIAL_COVERAGE")

    override fun getTextAttributesKey(): TextAttributesKey {
        return textAttributesKey
    }

}


/**
 * TODO removed this?
 */
object leanFileProgressFinishedFillingLineMarkerRender : FillingLineMarkerRenderer {
    override fun getPosition(): LineMarkerRendererEx.Position {
        return LineMarkerRendererEx.Position.LEFT
    }

    /**
     * TODO the reason for keys is that it's related to theme
     *      use our own key here
     */
    private val textAttributesKey = TextAttributesKey.createTextAttributesKey("LEAN_FILE_PROGRESS_FINISHED")
    // private val textAttributesKey = TextAttributesKey.createTextAttributesKey("LINE_PARTIAL_COVERAGE")

    override fun getTextAttributesKey(): TextAttributesKey {
        return textAttributesKey
    }

}

class LeanFile(private val leanProjectService: LeanProjectService, private val file: String) {

    companion object {
        val progressingLineMarker = DefaultLineMarkerRenderer(
            TextAttributesKey.createTextAttributesKey("LINE_PARTIAL_COVERAGE"), 8, 0, LineMarkerRendererEx.Position.LEFT
        )
    }

    /**
     * TODO this should be better named
     */
    private val unquotedFile = LspUtil.unquote(file)
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
            while (true) {
                var info = processingInfoChannel.receive()
                var processingLineMarker = mutableListOf<RangeHighlighter>()
                processingLineMarker = tryAddLineMarker(info, processingLineMarker)
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
                            processingLineMarker = tryAddLineMarker(info, processingLineMarker)
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
    }

    /**
     * TODO rather than one line highlight. Highlight it on range
     *      for avoiding flashing, or performance issue
     */
    private fun tryAddLineMarker(
        info: FileProgressProcessingInfo,
        processingLineMarker: MutableList<RangeHighlighter>
    ): MutableList<RangeHighlighter> {
        val ret = mutableListOf<RangeHighlighter>()
        FileEditorManager.getInstance(project).selectedTextEditor?.let { editor ->
            if (editor.virtualFile.path == unquotedFile) {
                val document = editor.document
                for (processingLinerMarker in processingLineMarker) {
                    editor.markupModel.removeHighlighter(processingLinerMarker)
                }
                for (processingInfo in info.processing) {
                    for (i in processingInfo.range.start.line..processingInfo.range.end.line) {
                        // this is copied from com/intellij/openapi/editor/impl/MarkupModelImpl.java:69
                        if (!DocumentUtil.isValidLine(i, document)) {
                            continue
                        }
                        val rangeHighlighter = editor.markupModel.addLineHighlighter(i, HighlighterLayer.LAST, null)
                        // rangeHighlighter.gutterIconRenderer = FileProgressGutterIconRender()
                        rangeHighlighter.lineMarkerRenderer = leanFileProgressFillingLineMarkerRender
                        ret.add(rangeHighlighter)
                    }
                }
                if (ret.isEmpty()) {
                    // add a default line marker for avoiding the editor flashing left/right ...
                    // TODO there maybe some better way
                    if (DocumentUtil.isValidLine(0, document)) {
                        val rangeHighlighter = editor.markupModel.addLineHighlighter(0, HighlighterLayer.LAST, null)
                        rangeHighlighter.lineMarkerRenderer = leanFileProgressFinishedFillingLineMarkerRender
                        ret.add(rangeHighlighter)
                    }
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
     */
    fun updateCaret(logicalPosition: LogicalPosition) {
        val position = Position(line = logicalPosition.line, character = logicalPosition.column)
        val params = PlainGoalParams(TextDocumentIdentifier(LspUtil.quote(file)), position)
        leanProjectService.updateCaret(params)
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
            sessionMutex.withLock {
                if (oldSession == session) {
                    session = leanProjectService.languageServer.await().rpcConnect(RpcConnectParams(file)).sessionId
                    // keep alive making infoToInteractive behave better, for the reference must have the same session
                    // as the goal result, so keep it alive here...
                    keepAlive()
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

    suspend fun rpcCallRaw(params: RpcCallParamsRaw): JsonElement? {
        // TODO retry
        try {
            return leanProjectService.languageServer.await().rpcCall(params)
        } catch (ex: ResponseErrorException) {
            val responseError = ex.responseError
            // TODO remove this magic number and find lean source code for it
            if (responseError.code == -32900 && responseError.message == "Outdated RPC session") {
                // Here there is a possibility that rpcCallRaw is called concurrently and all of them failed
                // the lock in updateSession will avoid update session continuously
                // also check the comment inside updateSession, in fact we keep it alive forever...
                updateSession(params.sessionId)
                val paramsRetry = RpcCallParamsRaw(session!!, params.method, params.textDocument, params.position, params.params)
                return rpcCallRaw(paramsRetry)
            }
            if (responseError.code == -32603 && responseError.message == "elaboration interrupted") {
                return null
            }
            if (responseError.code == -32601 && responseError.message.contains("No RPC method")) {
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
            /**
             * {
             *   "code": -32801,
             *   "message": "The file worker for file:///I:/repos/CategoriesandHilbertSpaces/CHS/C0_Getting_Started/Test_2.lean has been terminated. Either the header has changed, or the file was closed,  or the server is shutting down."
             * }
             */
            /**
             * 2024-08-11 14:17:38,335 [ 624441]   WARN - org.eclipse.lsp4j.jsonrpc.RemoteEndpoint - Unmatched response message: {
             *   "jsonrpc": "2.0",
             *   "id": "142",
             *   "error": {
             *     "code": -32601,
             *     "message": "No RPC method \u0027Lean.Widget.getInteractiveDiagnostics\u0027 found"
             *   }
             * }
             */
            throw ex
        } catch (ex: Exception) {
            // org.eclipse.lsp4j.jsonrpc.ResponseErrorException: elaboration interrupted
            // TODO outdated session seems not reported here
            throw ex
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

    fun publishDiagnostics(diagnostics: List<Diagnostic>) {
        for (d in diagnostics) {
            buildWindowService.addBuildEvent(file, d.message)
        }
    }

}

