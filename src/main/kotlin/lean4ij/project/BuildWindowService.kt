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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

open class BuildEvent(val file: String)
class BuildStart(file: String): BuildEvent(file)
class BuildEnd(file: String): BuildEvent(file)
class BuildMessage(file: String, val message: String): BuildEvent(file)


/**
 * A simple service for build tool window
 */
@Service(Service.Level.PROJECT)
@Suppress("UnstableApiUsage")
class BuildWindowService(val project: Project) {

    private val leanProject : LeanProjectService = project.service()
    private val systemId = ProjectSystemId("LEAN4")
    private val syncId = ExternalSystemTaskId.create(systemId, ExternalSystemTaskType.RESOLVE_PROJECT, project)
    /**
     * Events are posted with [Channel.trySend] directly from the calling thread (not from a separately launched
     * coroutine per event), so the start/message/end order of a single caller is preserved. The channel is
     * unlimited so nothing is dropped if an event arrives before the collector below has started: a plain
     * [kotlinx.coroutines.flow.MutableSharedFlow] silently drops emissions while it has no subscribers, which
     * produced "no build for <file>" errors for the external infoview's start-up build.
     */
    private val events = Channel<BuildEvent>(Channel.UNLIMITED)
    private val builds = HashMap<String, BuildProgress<*>>()

    init {
        leanProject.scope.launch {
            /**
             * TODO check createBuildProgress method
             * and https://github.com/JetBrains/intellij-community/blob/1d45fcdd827f7bc3fde15d7eda2b4399780fb632/platform/lang-impl/testSources/com/intellij/build/BuildViewTest.kt#L50
             */
            var progress : BuildProgress<BuildProgressDescriptor>? = null

            // single consumer, events are handled strictly sequentially
            for (s in events) {
                (s as? BuildStart)?.let {
                    if (progress == null) {
                        progress = createProgress()
                    }
                    // TODO there seems still some duplicated start events...
                    builds.computeIfAbsent(s.file) {
                        progress!!.startChildProgress(s.file)
                    }
                }
                (s as? BuildEnd)?.let {
                    val build = builds.remove(s.file)
                    if (build == null) {
                        // Not an error worth a fatal-error dialog: the server may report an end without a start
                        thisLogger().warn("no build for ${s.file}")
                    } else {
                        build.finish()
                    }
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
                        // TODO better way for handling this
                        fileProgress.cancel()
                        builds.remove(s.file)
                        thisLogger().warn("${s.file} build failed with message:\n${s.message}")
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
        events.trySend(BuildStart(leanProject.getRelativePath(LspUtil.unquote(file))))
    }

    fun endBuild(file: String) {
        events.trySend(BuildEnd(leanProject.getRelativePath(LspUtil.unquote(file))))
    }

    fun addBuildEvent(file: String, message: String) {
        events.trySend(BuildMessage(leanProject.getRelativePath(LspUtil.unquote(file)), message))
    }

}
