package com.github.onriv.ijpluginlean.lsp

import com.github.onriv.ijpluginlean.lsp.data.FileProgressProcessingInfo
import com.github.onriv.ijpluginlean.project.LspService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportProgress
import com.intellij.platform.util.progress.withProgressText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

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

    /**
     * This and the following startFileProgressTask seems ugly
     */
    @Synchronized
    fun update(info: FileProgressProcessingInfo) {
        if (!processing) {
            if  (info.processing.isEmpty()) {
                return
            }
            scope.launch {
                withBackgroundProgress(project, LspConstants.FILE_PROGRESS, false) {
                    withProgressText(file) {
                        reportProgress {reporter ->
                            var workSize = 0
                            reporter.sizedStep(workSize) {  }
                            while (workSize != 100) {
                                val info = processingInfoChannel.receive()
                                workSize = if (info.processing.isEmpty()) {
                                    100
                                } else {
                                    info.processing[0].range.start.line*100/info.processing[0].range.end.line
                                }
                                reporter.sizedStep(workSize) {  }
                            }
                        }
                    }
                    processing = false
                }
            }
            processing = true
        }
        scope.launch {
            processingInfoChannel.send(info)
        }
    }

    fun isProcessing() = processing


}