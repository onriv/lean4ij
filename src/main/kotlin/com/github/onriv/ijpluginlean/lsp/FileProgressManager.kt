package com.github.onriv.ijpluginlean.lsp

import com.github.onriv.ijpluginlean.lsp.data.FileProgressProcessingInfo
import com.github.onriv.ijpluginlean.project.LspService
import com.github.onriv.ijpluginlean.services.SseEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.ProgressReporter
import com.intellij.platform.util.progress.reportProgress
import com.intellij.platform.util.progress.withProgressText
import io.ktor.server.application.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

suspend fun ProgressReporter.step(size: Int) {
    sizedStep(size) {}
}

class FileProgress(val project: Project, val file: String) {

    companion object {
        private val progresses: ConcurrentHashMap<String, FileProgress> = ConcurrentHashMap()

        fun run(project: Project, info: FileProgressProcessingInfo) {
            val fp = progresses.computeIfAbsent(info.textDocument.uri) {
                FileProgress(project, info.textDocument.uri)
            }
            fp.update(info)
        }

        fun getFileProgress(file: String) = progresses[file]
    }

    private val processingInfoChannel = Channel<FileProgressProcessingInfo>()

    private val scope = project.service<LspService>().scope

    private var processing : Boolean = false

    init {
        scope.launch {
            while (true) {
                var info = processingInfoChannel.receive()
                if (info.isFinished()) {
                    continue
                }
                processing = true
                withBackgroundFileProgress {reporter ->
                    do {
                        val workSize = info.workSize()
                        reporter.step(workSize)
                        info = processingInfoChannel.receive()
                    } while (info.isProcessing())
                }
                processing = false
            }
        }
    }

    fun update(info: FileProgressProcessingInfo) {
        scope.launch {
            processingInfoChannel.send(info)
        }
    }

    private suspend fun withBackgroundFileProgress(action: suspend (reporter: ProgressReporter) -> Unit) {
        withBackgroundProgress(project, LspConstants.FILE_PROGRESS) {
            withProgressText(file) {
                reportProgress {reporter ->
                    action(reporter)
                }
            }
        }
    }


    fun isProcessing() = processing


}