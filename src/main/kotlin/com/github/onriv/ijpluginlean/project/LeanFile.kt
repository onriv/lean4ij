package com.github.onriv.ijpluginlean.project

import com.github.onriv.ijpluginlean.lsp.data.*
import com.github.onriv.ijpluginlean.util.Constants
import com.github.onriv.ijpluginlean.util.LspUtil
import com.github.onriv.ijpluginlean.util.step
import com.google.gson.JsonElement
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.ProgressReporter
import com.intellij.platform.util.progress.reportProgress
import com.intellij.platform.util.progress.withProgressText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.TextDocumentIdentifier

class LeanFile(private val leanProjectService: LeanProjectService, private val file: String) {

    private val processingInfoChannel = Channel<FileProgressProcessingInfo>()
    private val project = leanProjectService.project
    private val scope = leanProjectService.scope
    private val scopeIO = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            while (true) {
                var info = processingInfoChannel.receive()
                if (info.isFinished()) {
                    continue
                }
                withBackgroundFileProgress {reporter ->
                    do {
                        val workSize = info.workSize()
                        reporter.step(workSize)
                        info = processingInfoChannel.receive()
                    } while (info.isProcessing())
                }
            }
        }
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
            keepAlive()
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
        TODO("Not yet implemented")
    }

    suspend fun rpcCallRaw(params: PrcCallParamsRaw): JsonElement? {
        // TODO retry
        try {
            return leanProjectService.languageServer.await().rpcCall(params)
        } catch(ex: Exception) {
            throw ex
        }
    }

}