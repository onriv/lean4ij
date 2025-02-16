/**
 * TODO this definitely should DRY with ElanRunConfigurationType
 *      and LeanRunConfigurationType
 */
package lean4ij.run

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesComponent
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
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.EditorTextField
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty
import com.intellij.util.ui.FormBuilder
import lean4ij.project.ElanService
import javax.swing.JComponent
import javax.swing.JPanel

fun <T : JComponent> Row.fullWidthCell(component: T): Cell<T> {
    return cell(component).align(Align.FILL)
}

/**
 * Copy from https://plugins.jetbrains.com/docs/intellij/run-configurations-tutorial.html#implement-a-configurationtype
 * The general logic seems to be [ElanRunConfigurationType] creates [ElanConfigurationFactory],
 * and factory creates [ElanRunConfiguration]
 * TODO copy from [ElanRunConfiguration]... should do some refactoring
 */
class ElanRunConfigurationType() : ConfigurationTypeBase(
    "ElanCommandConfiguration",
    "Elan",
    "Elan Command Configuration Type",
    NotNullLazyValue.createValue { AllIcons.Nodes.Console }
) {
    init {
        addFactory(ElanConfigurationFactory(this));
    }
}

class ElanConfigurationFactory(configurationType: ElanRunConfigurationType) : ConfigurationFactory(configurationType) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return ElanRunConfiguration(project, this, "Elan")
    }

    override fun getId() = "Elan"

    override fun getOptionsClass(): Class<out BaseState> {
        return ElanRunConfigurationOptions::class.java
    }
}

class ElanRunConfiguration( project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<ElanRunConfigurationOptions>(project, factory, name) {

    /**
     * In the tutorial,
     * https://github.com/JetBrains/intellij-sdk-code-samples/blob/main/run_configuration/src/main/java/org/jetbrains/sdk/runConfiguration/DemoRunConfiguration.java#L28
     * the getOptions method is already converting super.getOptions to the target type
     * Don't know why I am creating new instances every time here earlier. Maybe I took it wrong somewhere
     */
    public override fun getOptions(): ElanRunConfigurationOptions =
        super.getOptions() as ElanRunConfigurationOptions


    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return ElanRunSettingsEditor()
    }

    override fun getState(
        executor: Executor,
        environment: ExecutionEnvironment
    ): RunProfileState {
        val toolchainService = project.service<ElanService>()
        val commandLine: GeneralCommandLine = toolchainService.commandForRunningElan(options.arguments, project, options.environments)
        return ElanRunState(commandLine, environment)
    }

}

class ElanRunState(private val commandLine: GeneralCommandLine, environment: ExecutionEnvironment) : CommandLineState(environment) {
    override fun startProcess(): ProcessHandler {
        val processHandler = ProcessHandlerFactory.getInstance()
            .createColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        return processHandler
    }
}

class ElanRunSettingsEditor : SettingsEditor<ElanRunConfiguration>() {
    private val argumentsField: RawCommandLineEditor = RawCommandLineEditor()
    private val environmentField : EnvironmentVariablesComponent = EnvironmentVariablesComponent()

    /**
     * TODO it seems no ui elements for this?
     */
    override fun resetEditorFrom(elanRunConfiguration: ElanRunConfiguration) {
        argumentsField.text = elanRunConfiguration.options.arguments
        environmentField.envs = elanRunConfiguration.options.environments
    }

    override fun applyEditorTo(elanRunConfiguration: ElanRunConfiguration) {
        elanRunConfiguration.options.arguments = argumentsField.text
        // TODO should parent envs be manually handled?
        elanRunConfiguration.options.environments = environmentField.envs
    }

    /**
     * TODO it seems the property here and the following binding is unnecessary
     */
    var argumentsProp: String
        get() = argumentsField.text
        set(value) {
            argumentsField.text = value
        }

    var environmentsProp : Map<String, String>
        get() = environmentField.envs
        set(value) {
            environmentField.envs = value
        }


    override fun createEditor(): JComponent {
        return panel {
            row("&Arguments") {
                fullWidthCell(argumentsField)
                    .resizableColumn()
                    .comment("Arguments for elan")
                    // TODO the binding seems unnecessary
                    .bind (
                        componentGet = {it.text},
                        componentSet = {component, value -> component.text = value},
                        // TODO this is a comment for kotlin: understand the use of toMutableProperty
                        //      why it requires the method toMutableProperty
                        prop = this@ElanRunSettingsEditor::argumentsProp.toMutableProperty()
                    )
            }
            row("&Environment Variables") {
                fullWidthCell(environmentField)
                    .comment("Environment variables for running elan")
                    .bind(
                        componentGet = {it.envs},
                        componentSet = {component, value -> component.envs = value},
                        prop = this@ElanRunSettingsEditor::environmentsProp.toMutableProperty()
                    )
            }
        }
    }
}

/**
 * The class [RunConfigurationOptions] use annotation for xml tag, don't know if it's relevant here or not
 * but the example in run-configurations-tutorial use property.
 */
class ElanRunConfigurationOptions : RunConfigurationOptions() {

    // TODO this is a comment for kotlin: understand the use of provideDelegate
    //      and https://kotlinlang.org/docs/delegated-properties.html#local-delegated-properties
    private var argumentsOption = string("").provideDelegate(this, "arguments")

    private var environmentsOption = map<String, String>().provideDelegate(this, "environments")

    var arguments : String
        get() = argumentsOption.getValue(this) ?: ""
        set(value) = argumentsOption.setValue(this, value)

    var environments : MutableMap<String, String>
        get() = environmentsOption.getValue(this)
        set(value) {
            environmentsOption.setValue(this, value.toMutableMap())
        }

    // working directory is not applicable here, so not adding it
    // checking elan it seems the command must be run from the root of the project
    // TODO add environments
}
