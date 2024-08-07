package com.github.onriv.ijpluginlean.project

import com.github.onriv.ijpluginlean.lsp.data.*
import com.github.onriv.ijpluginlean.util.Constants
import com.github.onriv.ijpluginlean.util.LspUtil
import com.github.onriv.ijpluginlean.util.step
import com.google.gson.JsonElement
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.ProgressReporter
import com.intellij.platform.util.progress.reportProgress
import com.intellij.platform.util.progress.withProgressText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import java.nio.charset.StandardCharsets

class LeanFile(private val leanProjectService: LeanProjectService, private val file: String) {

    /**
     * TODO this should be better named
     */
    private val unquotedFile = LspUtil.unquote(file)
    private val processingInfoChannel = Channel<FileProgressProcessingInfo>()
    private val project = leanProjectService.project
    private val buildWindowService : BuildWindowService = project.service()
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

    private fun tryAddLineMarker(info: FileProgressProcessingInfo, processingLineMarker: MutableList<RangeHighlighter>) : MutableList<RangeHighlighter> {
        val ret = mutableListOf<RangeHighlighter>()
        FileEditorManager.getInstance(project).selectedTextEditor?.let {editor ->
            if (editor.virtualFile.path == unquotedFile) {
                for (processingLinerMarker in processingLineMarker) {
                    editor.markupModel.removeHighlighter(processingLinerMarker)
                }
                for (processingInfo in info.processing) {
                    for (i in processingInfo.range.start.line ..  processingInfo.range.end.line) {
                        val rangeHighlighter =
                            editor.markupModel.addLineHighlighter(i, HighlighterLayer.LAST, null)
                        rangeHighlighter.gutterIconRenderer = FileProgressGutterIconRender()
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
                reportProgress {reporter ->
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
        val position = Position(line=logicalPosition.line, character = logicalPosition.column)
        val params = PlainGoalParams(TextDocumentIdentifier(LspUtil.quote(file)), position)
        leanProjectService.updateCaret(params)
    }

    fun updateFileProcessingInfo(info: FileProgressProcessingInfo) {
        scope.launch {
            processingInfoChannel.send(info)
        }
    }

    private var session : String? = null
    suspend fun getSession(forceUpdate: Boolean = false) : String {
        if (session == null) {
            session = leanProjectService.languageServer.await().rpcConnect(RpcConnectParams(file)).sessionId
            // keepAlive()
        }
        return session!!
    }

    /**
     * TODO maybe it should not always keep alive
     */
    private fun keepAlive() {
        scopeIO.launch {
            while (true) {
                delay(9*1000)
                leanProjectService.languageServer.await().rpcKeepAlive(RpcKeepAliveParams(file, session!!))
            }
        }
        // TODO this is in fact unreachable? since it's throwing an error
        TODO("Not yet implemented")
    }

    suspend fun rpcCallRaw(params: RpcCallParamsRaw): JsonElement? {
        // TODO retry
        try {
            return leanProjectService.languageServer.await().rpcCall(params)
        } catch (ex: ResponseErrorException) {
            val responseError = ex.responseError
            // TODO remove this magic number and find lean source code for it
            if (responseError.code == -32900 && responseError.message == "Outdated RPC session") {
                getSession(forceUpdate = true)
                val paramsRetry = RpcCallParamsRaw(session!!, params.method, params.textDocument, params.position, params.params)
                return rpcCallRaw(paramsRetry)
            }
            if (responseError.code == -32603 && responseError.message == "elaboration interrupted") {
                return null
            }
            throw ex
        } catch(ex: Exception) {
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

}

