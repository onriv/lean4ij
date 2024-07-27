package com.github.onriv.ijpluginlean.actions

import com.github.onriv.ijpluginlean.lsp.data.FileProgressProcessingInfo
import com.github.onriv.ijpluginlean.util.Lean4Util.runCommand
import com.google.common.base.Stopwatch
import com.intellij.build.*
import com.intellij.build.events.MessageEvent.Kind.*
import com.intellij.build.events.PresentableBuildEvent
import com.intellij.build.events.impl.*
import com.intellij.build.events.impl.FinishBuildEventImpl
import com.intellij.build.events.impl.OutputBuildEventImpl
import com.intellij.build.events.impl.StartBuildEventImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.build.progress.BuildProgress
import com.intellij.build.progress.BuildProgressDescriptor
import com.intellij.build.progress.BuildRootProgressImpl
import com.intellij.lang.LangBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


open class FileProgressViewManager(project: Project) : AbstractViewManager(project) {
    public override fun getViewName(): String {
        return "FileProgress"
    }

    companion object {
        fun createBuildProgress(project: Project): BuildProgress<BuildProgressDescriptor> {
            return BuildRootProgressImpl(project.getService(FileProgressViewManager::class.java))
        }
    }
}

class BuildWindowManager(val project: Project) {

    companion object {
        // TODO use intellij idea's service maybe
        private val instances = ConcurrentHashMap<Project, BuildWindowManager>()

        fun getInstance(project: Project) : BuildWindowManager {
            return instances.computeIfAbsent(project) {BuildWindowManager(it)}
        }
    }

    val syncView = FileProgressViewManager(project)

    private val systemId = ProjectSystemId("LEAN4")
    val syncId = ExternalSystemTaskId.create(systemId, ExternalSystemTaskType.RESOLVE_PROJECT, project)
    var  progres : BuildProgress<BuildProgressDescriptor>? = null
    private val processingFiles = ConcurrentHashMap<String, BuildProgress<BuildProgressDescriptor>>()

    fun fileProgress() {
        val descriptor = DefaultBuildDescriptor(syncId, "fileProgress", project.basePath!!, System.currentTimeMillis())
        // var fileProgressEvent = StartBuildEventImpl(descriptor, "")
        // syncView.onEvent(descriptor, fileProgressEvent)
        // val curldescriptor = DefaultBuildDescriptor(descriptor, syncId, "curl", project.basePath!!, System.currentTimeMillis())
        // val curlVersionOutput = runCommand("curl --version")
        // // syncView.onEvent(descriptor, OutputBuildEventImpl(syncId, curlVersionOutput!!, true))
        // // syncView.onEvent(descriptor, FinishBuildEventImpl(syncId, syncId, System.currentTimeMillis(), "--version", SuccessResultImpl()))
        // syncView.onEvent(descriptor, OutputBuildEventImpl(curldescriptor, curlVersionOutput!!, true))

        // TODO use service  check createBuildProgress method
        // and https://github.com/JetBrains/intellij-community/blob/1d45fcdd827f7bc3fde15d7eda2b4399780fb632/platform/lang-impl/testSources/com/intellij/build/BuildViewTest.kt#L50
        progres = BuildRootProgressImpl(syncView)
            .start(object : BuildProgressDescriptor {
                override fun getTitle(): String {
                    return "File Progress"
                }
                override fun getBuildDescriptor(): BuildDescriptor {
                    return descriptor
                }
            })
        progres!!.progress("Running…")
        // TODO do it kotlin way
        // object : Thread() {
        //     override fun run() {
        //         sleep(10*1000)
        //
        //     }
        // }.start()
        //     .startChildProgress("curl")
        //     .message("curl --version", runCommand("curl --version")!!, SIMPLE, null)
        // progres!!.finish()

            // .startChildProgress("Inner progress")
            // .fileMessage("File message1", "message1 descriptive text", INFO, FilePosition(File("aFile.java"), 0, 0))
            // .fileMessage("File message2", "message2 descriptive text", INFO, FilePosition(File("aFile.java"), 0, 0))
            // .finish()
    }

    fun fileProcess(info: FileProgressProcessingInfo)  {
        if (progres == null) {
            return
        }
        val fileProgre = processingFiles.computeIfAbsent(info.textDocument.uri) {
            progres!!.startChildProgress(info.textDocument.uri)
        }
        if (info.processing.isEmpty()) {
            fileProgre.finish()
            processingFiles.remove(info.textDocument.uri)
        } else {
            fileProgre.progress(info.textDocument.uri)
        }

    }

}