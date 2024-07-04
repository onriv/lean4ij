package com.github.onriv.ijpluginlean.lsp

import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap

class FileProgress(val project: Project, file: String) {

    companion object {
        val progresses: ConcurrentHashMap<String, FileProgress> = ConcurrentHashMap()

        fun run(info: LeanFileProgressProcessingInfo) {
            val project = ProjectManager.getInstance().openProjects.find {
                info.textDocument.uri.startsWith(
                    LeanLspServerManager.tryFixWinUrl("file://"+it.basePath!!))}
            LeanLspServerManager.getInstance(project!!).startImport()
            val fp = progresses.computeIfAbsent(info.textDocument.uri) {
                FileProgress(project!!, info.textDocument.uri)
            }
            fp.update(info)
        }
    }

    init {
        runBackgroundableTask(file, project) {
            while (true) {
                val info = channel.poll()
                if (info.processing.isEmpty()) {
                    it.checkCanceled();
                    it.fraction = 1.0
                    break
                }
                it.checkCanceled();
                it.fraction = info.processing[1].range.start.line.toDouble()/ info.processing[1].range.end.line.toDouble()
            }
        }
    }

    // TODO do it kotlin way
    private val channel = ArrayBlockingQueue<LeanFileProgressProcessingInfo>(1000)

    fun update(info: LeanFileProgressProcessingInfo) {
        channel.add(info)
    }


}