package com.github.onriv.ijpluginlean.project

import com.github.onriv.ijpluginlean.util.Constants
import com.github.onriv.ijpluginlean.util.LspUtil
import com.intellij.build.AbstractViewManager
import com.intellij.build.BuildDescriptor
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.progress.BuildProgress
import com.intellij.build.progress.BuildProgressDescriptor
import com.intellij.build.progress.BuildRootProgressImpl
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A simple service for build tool window
 */
@Service(Service.Level.PROJECT)
@Suppress("UnstableApiUsage")
class BuildWindowService(val project: Project) {

    private val syncView = object : AbstractViewManager(project) {
        public override fun getViewName(): String {
            return Constants.FILE_PROGRESS
        }
    }

    private val leanProject : LeanProjectService = project.service()
    private val systemId = ProjectSystemId("LEAN4")
    private val syncId = ExternalSystemTaskId.create(systemId, ExternalSystemTaskType.RESOLVE_PROJECT, project)
    private var flow = MutableSharedFlow<String>()

    init {
        leanProject.scope.launch {
            /**
             * TODO check createBuildProgress method
             * and https://github.com/JetBrains/intellij-community/blob/1d45fcdd827f7bc3fde15d7eda2b4399780fb632/platform/lang-impl/testSources/com/intellij/build/BuildViewTest.kt#L50
             */
            var progress : BuildProgress<BuildProgressDescriptor>? = null
            val builds = HashMap<String, BuildProgress<*>>()
            val mutex = Mutex()

            // TODO is the mutex here really necessary?
            flow.collect { s ->
                mutex.withLock {
                    // TODO rather than using string, use class for this
                    // TODO never mind, keep it running
                    // if (s == "--") {
                    //     if (builds.isEmpty()) {
                    //         progress!!.finish()
                    //         progress = null
                    //     }
                    // } else
                    if (!s.startsWith("-")) {
                        if (progress == null) {
                            progress = createProgress()
                        }
                        builds.put(s, progress!!.startChildProgress(s))
                    } else if (s.startsWith("-")) {
                        val s1 = s.substring(1)
                        builds[s1]!!.finish()
                        builds.remove(s1)
                        // launch {
                        //     delay(10 * 1000)
                        //     flow.emit("--")
                        // }
                    }
                }
            }
        }
    }

    fun createProgress(): BuildProgress<BuildProgressDescriptor> {
        val descriptor = DefaultBuildDescriptor(syncId, Constants.FILE_PROGRESS, project.basePath!!, System.currentTimeMillis())
        return  BuildRootProgressImpl(syncView)
            .start(object : BuildProgressDescriptor {
                override fun getTitle(): String {
                    return Constants.FILE_PROGRESS
                }
                override fun getBuildDescriptor(): BuildDescriptor {
                    return descriptor
                }
            })
    }

    fun startBuild(file: String) {
        leanProject.scope.launch {
            flow.emit(leanProject.getRelativePath(LspUtil.unquote(file)))
        }
    }

    fun endBuild(file: String) {
        leanProject.scope.launch {
            flow.emit("-$file")
        }
    }

}