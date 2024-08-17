package lean4ij.project

import lean4ij.infoview.external.ExternalInfoViewService
import lean4ij.project.listeners.LeanFileCaretListener
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEventMulticasterEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.startup.ProjectActivity
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import com.intellij.openapi.module.Module
import lean4ij.lsp.LeanLanguageServerFactory
import java.awt.event.FocusEvent

fun Module.addExcludeFolder(basePath: String) {
    ModuleRootModificationUtil.updateModel(this) { rootModule ->
        // check https://github.com/intellij-rust/intellij-rust/issues/1062<
        // TODO any way not use Sdk or make a Sdk for lean?
        rootModule.inheritSdk()
        val contentEntries = rootModule.contentEntries
        contentEntries.singleOrNull()?.let { contentEntry ->
            // TODO excludeFolder seems still in Find
            contentEntry.addExcludePattern(".olean")
            contentEntry.addExcludePattern(".ilean")
            contentEntry.addExcludePattern(".c")
            Files.walk(Path.of(basePath, ".lake"), 5)
                .filter { path -> path.isDirectory() }
                .forEach { path ->
                    thisLogger().info("checking if $path should be excluded")
                    if (path.parent.name == ".lake" && path.name == "build" ) {
                        thisLogger().info("adding $path to excludeFolder")
                        // must be of pattern "file://", the last replace is for fixing path in Windows...
                        val uri = path.toUri().toString().replace("file:///", "file://")
                        try {
                            // TODO it seems no working...
                            //      really weird, it seems working
                            contentEntry.addExcludeFolder(uri)
                            // TODO here thisLogger is Module
                            //      maybe it's better not using extension method
                            thisLogger().info("$path excluded")
                        } catch (ex: Exception) {
                            thisLogger().error("cannot exclude $uri", ex)
                        }
                    }
                }
        }
        rootModule.project.save()
    }

}
/**
 * see: [defining-project-level-listeners](https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html#defining-project-level-listeners)
 * this is copied from [ConnectDocumentToLanguageServerSetupParticipant](https://github.com/redhat-developer/lsp4ij/blob/cb04e064f93ec8c2bb22b216e54b6a7fb1c75496/src/main/java/com/redhat/devtools/lsp4ij/ConnectDocumentToLanguageServerSetupParticipant.java#L29)
 * no more from the above, now it implements [ProjectActivity]
 * ref: https://plugins.jetbrains.com/docs/intellij/plugin-components.html#project-and-application-close
 * and https://github.com/JetBrains/intellij-sdk-code-samples/blob/main/max_opened_projects/src/main/kotlin/org/intellij/sdk/maxOpenProjects/ProjectOpenStartupActivity.kt
 */
class LeanProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        setupModule(project)
        setupEditorFocusChangeEventListener()
        project.service<LeanProjectService>()
        project.service<LeanFileCaretListener>()
        project.service<ExternalInfoViewService>()
    }

    /**
     * ref: https://intellij-support.jetbrains.com/hc/en-us/community/posts/4578776718354-How-do-I-listen-for-editor-focus-events
     * TODO absolutely this requires some refactor
     *      this is for avoiding didOpen request that make the lean lsp server handling it and improve performance
     *      but it may have some false positive event though
     */
    private fun setupEditorFocusChangeEventListener() {
        (EditorFactory.getInstance().eventMulticaster as? EditorEventMulticasterEx)?.let { ex ->
            ex.addFocusChangeListener(object: FocusChangeListener {
                override fun focusGained(editor: Editor) {
                    LeanLanguageServerFactory.isEnable.set(true)
                }

                override fun focusGained(editor: Editor, event: FocusEvent) {
                    LeanLanguageServerFactory.isEnable.set(true)
                }

                override fun focusLost(editor: Editor) {
                    LeanLanguageServerFactory.isEnable.set(false)
                }

                override fun focusLost(editor: Editor, event: FocusEvent) {
                    LeanLanguageServerFactory.isEnable.set(false)
                }
            }) {
                // TODO add real Disposable, it's used for avoiding resource leak
                //      check com.intellij.codeInsight.daemon.impl.EditorTrackerImpl
                //      and the doc https://plugins.jetbrains.com/docs/intellij/disposers.html
            }
        }
    }

    /**
     * Mostly this method is from
     * intellij-rust/src/main/kotlin/org/rust/ide/module/CargoConfigurationWizardStep.kt" 100 lines
     * for setting up module for ignoring .lake/build for index
     * check also intellij-rust/intellij
     */
    private fun setupModule(project: Project) {
        project.basePath?.let{ basePath ->
            project.projectFile?.let {
                thisLogger().info("current module is $it")
                // logger shows .idea/misc.xml
                val module = ModuleUtilCore.findModuleForFile(it, project)
                module?.addExcludeFolder(basePath)
            }
        }
    }

}