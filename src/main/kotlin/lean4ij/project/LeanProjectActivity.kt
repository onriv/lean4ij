package lean4ij.project

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEventMulticasterEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import lean4ij.infoview.external.ExternalInfoViewService
import lean4ij.project.listeners.Lean4DocumentListener
import lean4ij.project.listeners.Lean4EditorFocusChangeListener
import lean4ij.project.listeners.LeanFileCaretListener
import lean4ij.sdk.SdkService

/**
 * see: [defining-project-level-listeners](https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html#defining-project-level-listeners)
 * this is copied from [ConnectDocumentToLanguageServerSetupParticipant](https://github.com/redhat-developer/lsp4ij/blob/cb04e064f93ec8c2bb22b216e54b6a7fb1c75496/src/main/java/com/redhat/devtools/lsp4ij/ConnectDocumentToLanguageServerSetupParticipant.java#L29)
 * no more from the above, now it implements [ProjectActivity]
 * ref: https://plugins.jetbrains.com/docs/intellij/plugin-components.html#project-and-application-close
 * and https://github.com/JetBrains/intellij-sdk-code-samples/blob/main/max_opened_projects/src/main/kotlin/org/intellij/sdk/maxOpenProjects/ProjectOpenStartupActivity.kt
 */
class LeanProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        val leanProject = project.service<LeanProjectService>()
        if (!leanProject.isLeanProject()) {
            return
        }
        if (!leanProject.isProjectCreating){
            project.service<SdkService>().setupModule()
        }

        setupEditorFocusChangeEventListener(project)

        project.service<LeanProjectService>()
        project.service<LeanFileCaretListener>()
        project.service<ExternalInfoViewService>()
    }

    private fun setupEditorFocusChangeEventListener(project: Project) {
        (EditorFactory.getInstance().eventMulticaster as? EditorEventMulticasterEx)?.let {
            Lean4EditorFocusChangeListener().register(it)
            Lean4DocumentListener(project).register(it)
        }
    }

}
