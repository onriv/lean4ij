package lean4ij.project

import lean4ij.util.Constants
import lean4ij.util.LspUtil
import com.intellij.build.AbstractViewManager
import com.intellij.build.BuildDescriptor
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.SyncViewManager
import com.intellij.build.progress.BuildProgress
import com.intellij.build.progress.BuildProgressDescriptor
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Base class for build events, identified by the file being built. */
open class BuildEvent(val file: String)
/** Signals that compilation of [file] has started. */
class BuildStart(file: String): BuildEvent(file)
/** Signals that compilation of [file] has finished. */
class BuildEnd(file: String): BuildEvent(file)
/** A diagnostic or log message emitted during compilation of [file]. */
class BuildMessage(file: String, val message: String): BuildEvent(file)


/**
 * Manages the Build tool window to display Lean file compilation progress.
 *
 * This service bridges Lean's `$/lean/fileProgress` notifications with IntelliJ's Build
 * tool window API ([SyncViewManager]). It provides a familiar "build output" experience
 * where each file being elaborated appears as a child node with its diagnostic messages.
 *
 * ## Architecture
 * Uses a [MutableSharedFlow] of [BuildEvent]s to decouple event producers (LSP notifications)
 * from the UI consumer. Events are collected in a coroutine that manages the lifecycle of
 * [BuildProgress] nodes:
 * - [BuildStart] creates a new child progress node for the file
 * - [BuildMessage] appends output text to the file's progress node
 * - [BuildEnd] finishes (closes) the file's progress node
 *
 * ## Error Detection
 * If a build message contains "error: build failed", the corresponding file's progress
 * is cancelled and removed from tracking.
 *
 * @param project The IntelliJ project this service belongs to
 */
@Service(Service.Level.PROJECT)
@Suppress("UnstableApiUsage")
class BuildWindowService(val project: Project) {

    private val leanProject : LeanProjectService = project.service()
    private val systemId = ProjectSystemId("LEAN4")
    private val syncId = ExternalSystemTaskId.create(systemId, ExternalSystemTaskType.RESOLVE_PROJECT, project)
    private var flow = MutableSharedFlow<BuildEvent>()
    private val builds = HashMap<String, BuildProgress<*>>()

    init {
        leanProject.scope.launch {
            /**
             * TODO check createBuildProgress method
             * and https://github.com/JetBrains/intellij-community/blob/1d45fcdd827f7bc3fde15d7eda2b4399780fb632/platform/lang-impl/testSources/com/intellij/build/BuildViewTest.kt#L50
             */
            var progress : BuildProgress<BuildProgressDescriptor>? = null
            val mutex = Mutex()

            // TODO is the mutex here really necessary?
            // TODO here it seems blocking a thread
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
                    (s as? BuildStart)?.let {
                        if (progress == null) {
                            progress = createProgress()
                        }
                        // TODO there seems still some duplicated start events...
                        // builds[s.file] = progress!!.startChildProgress(s.file)
                        builds.computeIfAbsent(s.file) {
                            progress!!.startChildProgress(s.file)
                        }
                    }
                    (s as? BuildEnd)?.let {
                        val build = builds[s.file]
                        if (build == null) {
                            thisLogger().error("no build for ${s.file}")
                        }
                        // TODO there still seems build end without build start
                        builds[s.file]?.let {
                            it.finish()
                            builds.remove(s.file)
                        }
                        // launch {
                        //     delay(10 * 1000)
                        //     flow.emit("--")
                        // }
                    }
                    (s as? BuildMessage)?.let {
                        // TODO this can even before build start
                        if (progress == null) {
                            progress = createProgress()
                        }
                        val fileProgress = builds.computeIfAbsent(s.file) {
                            progress!!.startChildProgress(s.file)
                        }
                        fileProgress.output("${s.message}\n", false)
                        if (s.message.contains("error: build failed")) {
                            try {
                                // for (entry in builds.entries) {
                                //     entry.value.cancel()
                                // }
                                // TODO better way for handling this
                                fileProgress.cancel()
                                builds.remove(s.file)
                                // project.notifyErr("Build failed, check build window for detail:\n${s.message}")
                                thisLogger().warn("${s.file} build failed with message:\n${s.message}")
                            } catch(ex : Exception) {
                                throw ex
                            }

                        }
                    }
                }
            }
        }
    }

    fun createProgress(): BuildProgress<BuildProgressDescriptor> {
        val descriptor = DefaultBuildDescriptor(syncId, Constants.FILE_PROGRESS, project.basePath!!, System.currentTimeMillis())
        return  SyncViewManager.createBuildProgress(project)
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
            flow.emit(BuildStart(leanProject.getRelativePath(LspUtil.unquote(file))))
        }
    }

    fun endBuild(file: String) {
        leanProject.scope.launch {
            flow.emit(BuildEnd(leanProject.getRelativePath(LspUtil.unquote(file))))
        }
    }

    fun addBuildEvent(file: String, message: String) {
        leanProject.scope.launch {
            flow.emit(BuildMessage(leanProject.getRelativePath(LspUtil.unquote(file)), message))
        }
    }

}
