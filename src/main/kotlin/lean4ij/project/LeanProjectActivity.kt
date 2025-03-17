package lean4ij.project

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEventMulticasterEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import lean4ij.infoview.external.ExternalInfoViewService
import lean4ij.project.listeners.Lean4DocumentListener
import lean4ij.project.listeners.Lean4EditorFocusChangeListener
import lean4ij.project.listeners.LeanFileCaretListener
import lean4ij.run.LakeRunConfiguration
import lean4ij.run.LakeRunConfigurationType
import lean4ij.sdk.SdkService
import lean4ij.util.leanProjectService

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

        project.addRunConfigurations()
    }

    private fun setupEditorFocusChangeEventListener(project: Project) {
        (EditorFactory.getInstance().eventMulticaster as? EditorEventMulticasterEx)?.let {
            Lean4EditorFocusChangeListener().register(it)
            Lean4DocumentListener(project).register(it)
        }
    }

    /**
     * Add some run configurations automatically like
     *   - `lake exec cache get` only when depends on mathlib
     *   - `lake build`
     * etc
     * TODO DRY with [lean4ij.module.Lean4ModuleBuilder.createProject]
     * TODO check this: this method is ran every time the project open,
     *      but it seems OK to run this everytime
     */
    private fun Project.addRunConfigurations() {
        val leanProject = this.service<LeanProjectService>()
        if (leanProject.isDependingOnMathlib()) {
            // TODO if the configuration already added to the project,we currently does not have
            //      mechanism to delete it. It requires to be deleted manually by the user.
            addLakeRunConfiguration("lake exe cache get", "exe cache get")
        }
        addLakeRunConfiguration("lake build", "build")
        addLakeRunConfiguration("lake update", "update")
    }

    /**
     * TODO DRY DRY...
     */
    private fun Project.addLakeRunConfiguration(name: String, arguments: String) {
        val runManager = RunManager.getInstance(this)
        val lakeRunConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(LakeRunConfigurationType::class.java)
        val configuration =
            runManager.createConfiguration(name, lakeRunConfigurationType.configurationFactories[0])
        val elanRuhConfiguration = configuration.configuration as LakeRunConfiguration
        // TODO modify lean-toolchain file to the specified version

        elanRuhConfiguration.options.arguments = arguments
        // For some environments (like me) it requires adding proxy for the download
        runManager.addConfiguration(configuration)
    }

}
