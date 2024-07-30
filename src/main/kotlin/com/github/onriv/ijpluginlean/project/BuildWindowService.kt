package com.github.onriv.ijpluginlean.project

import com.github.onriv.ijpluginlean.util.Constants
import com.intellij.build.AbstractViewManager
import com.intellij.build.BuildDescriptor
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.progress.BuildProgress
import com.intellij.build.progress.BuildProgressDescriptor
import com.intellij.build.progress.BuildRootProgressImpl
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project

/**
 * A simple service for build tool window
 */
@Service(Service.Level.PROJECT)
class BuildWindowService(val project: Project) {

    private val syncView = object : AbstractViewManager(project) {
        public override fun getViewName(): String {
            return Constants.FILE_PROGRESS
        }
    }

    private val systemId = ProjectSystemId("LEAN4")
    private val syncId = ExternalSystemTaskId.create(systemId, ExternalSystemTaskType.RESOLVE_PROJECT, project)
    private val descriptor = DefaultBuildDescriptor(syncId, Constants.FILE_PROGRESS, project.basePath!!, System.currentTimeMillis())
    /**
     * TODO check createBuildProgress method
     * and https://github.com/JetBrains/intellij-community/blob/1d45fcdd827f7bc3fde15d7eda2b4399780fb632/platform/lang-impl/testSources/com/intellij/build/BuildViewTest.kt#L50
     */
    val progress : BuildProgress<BuildProgressDescriptor>

    init {
        // NOT easy to design this, give it up
        progress = BuildRootProgressImpl(syncView)
            // .start(object : BuildProgressDescriptor {
            //     override fun getTitle(): String {
            //         return Constants.FILE_PROGRESS
            //     }
            //     override fun getBuildDescriptor(): BuildDescriptor {
            //         return descriptor
            //     }
            // })
    }

}