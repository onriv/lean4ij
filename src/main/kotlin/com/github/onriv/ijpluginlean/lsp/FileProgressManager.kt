package com.github.onriv.ijpluginlean.lsp

import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap

class FileProgress(val project: Project, val file: String) {

    companion object {
        val progresses: ConcurrentHashMap<String, FileProgress> = ConcurrentHashMap()

        // fun run(info: LeanFileProgressProcessingInfo) {
        //     val project = ProjectManager.getInstance().openProjects.find {
        //         info.textDocument.uri.startsWith(
        //             LeanLspServerManager.tryFixWinUrl("file://"+it.basePath!!))}
        //     // don't need to do this... kind of hard to design, remove it now
        //     LeanLspServerManager.getInstance(project!!).startImport()
        //     val fp = progresses.computeIfAbsent(info.textDocument.uri) {
        //         FileProgress(project!!, info.textDocument.uri)
        //     }
        //     fp.update(info)
        // }

        fun getFileProgress(file: String) = progresses[file]
    }

    init {
    }

    // TODO do it kotlin way
    // private val channel = ArrayBlockingQueue<LeanFileProgressProcessingInfo>(1000)
    private val channel = ArrayBlockingQueue<Any>(1000)

    private var processing : Boolean = false

    /**
     * TODO do it kotlin way
     * This and the following startFileProgressTask seems ugly
     */
    @Synchronized
    fun update(info: Any) {
        if (!processing) {
            // TODO not sure if this is necessary
            // if  (info.processing.isEmpty()) {
            //     return
            // }
            startFileProgressTask()
            processing = true
        }
        channel.add(info)
    }

    fun isProcessing() = processing

    private fun startFileProgressTask() {
        runBackgroundableTask("\$lean/fileProgress", project) {
            // while (true) {
            //     val info = channel.take()
            //     // TODO move this to util, and file
            //     it.text = info.textDocument.uri.replace(LeanLspServerManager.tryFixWinUrl("file://"+project.basePath!!+"/"),"")
            //     it.checkCanceled()
            //     it.fraction = 0.0
            //     try {
            //         if (info.processing.isEmpty()) {
            //             it.checkCanceled()
            //             it.fraction = 1.0
            //             break
            //         }
            //         it.checkCanceled();
            //         // TODO is this always just one element in info.processing?
            //         it.fraction = info.processing[0].range.start.line.toDouble()/ info.processing[0].range.end.line.toDouble()
            //     } catch(e: Exception) {
            //         e.printStackTrace()
            //     }
            // }
            processing = false
        }
    }

}