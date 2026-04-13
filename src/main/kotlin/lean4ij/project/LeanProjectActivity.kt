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
 * Startup activity that initializes the Lean 4 plugin when a project opens.
 *
 * This is the entry point that bootstraps the entire plugin infrastructure for a project.
 * It runs as a [ProjectActivity] (the modern replacement for `StartupActivity`) and performs
 * the following initialization steps:
 *
 * 1. **Project detection**: Checks if this is a Lean project (has `lean-toolchain` file).
 *    If not, returns early without initializing any Lean services.
 * 2. **SDK setup**: Configures the module SDK via [SdkService] (skipped during project creation).
 * 3. **Editor listeners**: Registers [Lean4EditorFocusChangeListener] (controls LSP enablement
 *    based on editor focus) and [Lean4DocumentListener] (triggers infoview updates on edits).
 * 4. **Core services**: Initializes [LeanProjectService], [LeanFileCaretListener], and
 *    [ExternalInfoViewService] (which starts the embedded HTTP server for the JCEF infoview).
 * 5. **Run configurations**: Adds default Lake run configurations (`lake build`, `lake update`,
 *    and `lake exe cache get` for Mathlib projects).
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html#defining-project-level-listeners">
 *   IntelliJ Plugin Listeners documentation</a>
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
