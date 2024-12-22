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
import com.intellij.ui.EditorTextField
import com.intellij.util.ui.FormBuilder
import lean4ij.project.ToolchainService
import javax.swing.JComponent
import javax.swing.JPanel


/**
 * Copy from https://plugins.jetbrains.com/docs/intellij/run-configurations-tutorial.html#implement-a-configurationtype
 * The general logic seems to be [LeanRunConfigurationType] creates [LeanConfigurationFactory],
 * and factory creates [LeanRunConfiguration]
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

    /**
     * In the tutorial,
     * https://github.com/JetBrains/intellij-sdk-code-samples/blob/main/run_configuration/src/main/java/org/jetbrains/sdk/runConfiguration/DemoRunConfiguration.java#L28
     * the getOptions method is already converting super.getOptions to the target type
     * Don't know why I am creating new instances every time here earlier. Maybe I took it wrong somewhere
     */
    public override fun getOptions(): LeanRunConfigurationOptions =
        super.getOptions() as LeanRunConfigurationOptions


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
                val commandLine: GeneralCommandLine = toolchainService.commandLineForRunningLeanFile(options.fileName, options.arguments)
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
    private val leanFilePathField: TextFieldWithBrowseButton = TextFieldWithBrowseButton()
    private val argumentsField: EditorTextField = EditorTextField()

    init {
        leanFilePathField.addBrowseFolderListener(
            "Select Script File", null, null,
            FileChooserDescriptorFactory.createSingleFileDescriptor()
        )
        myPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Script file", leanFilePathField)
            .addLabeledComponent("Arguments", argumentsField)
            .panel
    }

    override fun resetEditorFrom(leanRunConfiguration: LeanRunConfiguration) {
        leanFilePathField.text = leanRunConfiguration.options.fileName
        argumentsField.text = leanRunConfiguration.options.arguments
    }

    override fun applyEditorTo(leanRunConfiguration: LeanRunConfiguration) {
        leanRunConfiguration.options.fileName = leanFilePathField.text
        leanRunConfiguration.options.arguments = argumentsField.text
    }

    override fun createEditor(): JComponent {
        return myPanel
    }
}

/**
 * The class [RunConfigurationOptions] use annotation for xml tag, don't know if it's relevant here or not
 * but the example in run-configurations-tutorial use property.
 */
class LeanRunConfigurationOptions : RunConfigurationOptions() {

    private var fileNameOption = string("").provideDelegate(this, "fileName")

    private var argumentsOption = string("").provideDelegate(this, "arguments")

    var fileName : String
        get() = fileNameOption.getValue(this) ?: ""
        set(value) = fileNameOption.setValue(this, value)

    var arguments : String
        get() = argumentsOption.getValue(this) ?: ""
        set(value) = argumentsOption.setValue(this, value)

    // working directory is not applicable here, so not adding it
    // checking lake it seems the command must be run from the root of the project
    // TODO add environments
}
