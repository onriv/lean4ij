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
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.util.ui.FormBuilder
import com.jetbrains.rd.generator.nova.PredefinedType.*
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

    override fun getOptionsClass(): Class<out BaseState> {
        return LeanRunConfigurationOptions::class.java
    }
}

class LeanRunConfiguration( project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<LeanRunConfigurationOptions?>(project, factory, name) {

    override fun getOptions(): LeanRunConfigurationOptions {
        return super.getOptions() as LeanRunConfigurationOptions
    }

    var scriptName: String = ""

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration?> {
        return LeanRunSettingsEditor()
    }

    override fun getState(
        executor: Executor,
        environment: ExecutionEnvironment
    ): RunProfileState {
        return object : CommandLineState(environment) {
            override fun startProcess(): ProcessHandler {
                val commandLine: GeneralCommandLine =
                    GeneralCommandLine(options.scriptName)
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

    init {
        scriptPathField.addBrowseFolderListener(
            "Select Script File", null, null,
            FileChooserDescriptorFactory.createSingleFileDescriptor()
        )
        myPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Script file", scriptPathField)
            .panel
    }

    override fun resetEditorFrom(leanRunConfiguration: LeanRunConfiguration) {
        scriptPathField.text = leanRunConfiguration.scriptName
    }

    override fun applyEditorTo(leanRunConfiguration: LeanRunConfiguration) {
        leanRunConfiguration.scriptName = scriptPathField.text
    }

    override fun createEditor(): JComponent {
        return myPanel
    }
}

class LeanRunConfigurationOptions : RunConfigurationOptions() {
    var scriptName = ""
}
