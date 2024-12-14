package lean4ij.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.xmlb.annotations.OptionTag
import lean4ij.project.ToolchainService
import javax.swing.JComponent
import javax.swing.JPanel


/**
 * Copy from https://plugins.jetbrains.com/docs/intellij/run-configurations-tutorial.html#implement-a-configurationtype
 */
class LeanRunConfigurationType() : ConfigurationTypeBase(
    "LeanRunConfiguration",
    "Lean",
    "Lean Run Configuration Type",
    NotNullLazyValue.createValue { AllIcons.Nodes.Console }
) {
    init {
        addFactory(LeanConfigurationFactory(this));
    }
}

class LeanConfigurationFactory(configurationType: LeanRunConfigurationType) : ConfigurationFactory(configurationType) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return LeanRunConfiguration(project, this, "Lean")
    }

    override fun getId() = "Lean"

    override fun getOptionsClass(): Class<out BaseState> {
        return LeanRunConfigurationOptions::class.java
    }
}

class LeanRunConfiguration( project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<LeanRunConfigurationOptions>(project, factory, name) {

    private val _options = LeanRunConfigurationOptions()

    override fun getOptionsClass(): Class<out RunConfigurationOptions> {
        return super.getOptionsClass()
    }

    public override fun getOptions(): LeanRunConfigurationOptions = _options

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return LeanRunSettingsEditor()
    }

    override fun getState(
        executor: Executor,
        environment: ExecutionEnvironment
    ): RunProfileState {
        return object : CommandLineState(environment) {
            override fun startProcess(): ProcessHandler {

                val toolchainService = project.service<ToolchainService>()

                val commandLine: GeneralCommandLine =
                    GeneralCommandLine(
                        toolchainService.leanPath.toString(),
                        "--run",
                        options.fileName
                    )
                val processHandler = ProcessHandlerFactory.getInstance()
                    .createColoredProcessHandler(commandLine)
                ProcessTerminatedListener.attach(processHandler)
                return processHandler
            }

        }
    }

}

class LeanRunSettingsEditor : SettingsEditor<LeanRunConfiguration>() {
    private val myPanel: JPanel
    private val scriptPathField: TextFieldWithBrowseButton = TextFieldWithBrowseButton()
    private val argumentsField = ExpandableTextField()
    private val workingDirectoryField = TextFieldWithBrowseButton()

    init {
        scriptPathField.addBrowseFolderListener(
            "Select Script File", null, null,
            FileChooserDescriptorFactory.createSingleFileDescriptor()
        )
        myPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Script file", scriptPathField)
            .addLabeledComponent("Argument", argumentsField)
            .addLabeledComponent("Working directory", workingDirectoryField)
            .panel
    }



    override fun resetEditorFrom(leanRunConfiguration: LeanRunConfiguration) {
        scriptPathField.text = leanRunConfiguration.options.fileName!!
        // argumentsField.text = leanRunConfiguration.options.arguments
        // workingDirectoryField.text = leanRunConfiguration.workingDirectory
    }

    override fun applyEditorTo(leanRunConfiguration: LeanRunConfiguration) {
        leanRunConfiguration.options.fileName = scriptPathField.text
        // leanRunConfiguration.arguments = argumentsField.text
        // leanRunConfiguration.workingDirectory = workingDirectoryField.text
    }

    override fun createEditor(): JComponent {
        return myPanel
    }

    override fun isReadyForApply(): Boolean {
        return scriptPathField.text.isNotEmpty()
    }
}

/**
 * TODO this class seems created multiple instances for a single run configuration, dont know why
 */
class LeanRunConfigurationOptions : RunConfigurationOptions() {

    @get:OptionTag(tag = "fileName")
    var fileName : String? by string("")

    // TODO add arguments and working directory
}
