/**
 * Some of the code is from the tutorial
 * https://plugins.jetbrains.com/docs/intellij/adding-new-steps.html
 * and from the intellij-arend plugin
 * and from https://github.com/vaadin/intellij-plugin
 * for creating project directly without next
 * check https://intellij-support.jetbrains.com/hc/en-us/community/posts/18353598849170-How-to-create-single-module-wizard-step-with-generic-Name-and-Location-direct-Create-no-wizard-steps
 * for the related discussion
 * The skeleton is from vaadin plugin
 * TODO this file still requires BIG refactor
 */
package lean4ij.module

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.ide.IdeBundle
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.joinCanonicalPath
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withPathToTextConvertor
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withTextToPathConvertor
import com.intellij.openapi.module.GeneralModuleType
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.observable.util.bind
import com.intellij.openapi.observable.util.toStringProperty
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.getCanonicalPath
import com.intellij.openapi.ui.getPresentablePath
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.UIBundle
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.CollapsibleRow
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import lean4ij.language.Lean4Icons
import lean4ij.project.ElanService
import lean4ij.project.LeanProjectService
import lean4ij.run.ElanRunConfiguration
import lean4ij.run.ElanRunConfigurationType
import lean4ij.run.fullWidthCell
import lean4ij.sdk.SdkService
import lean4ij.util.execute
import java.awt.Color
import java.io.File
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JComponent
import kotlin.concurrent.timerTask

val QUICK_STARTER_MODEL_KEY = Key<GraphProperty<QuickStarterModel?>>("lean4_quick_starter_model")

fun <T : JComponent> Panel.aligned(text: String, component: T, init: Cell<T>.() -> Unit = {}) = row(text) {
    cell(component).align(AlignX.FILL).init()
}

class QuickStarterModel(private val propertyGraph: PropertyGraph, private val wizardContext: WizardContext) :
    BaseState() {

    companion object {
        val TEMPLATES = listOf("std", "exe", "lib", "math")
        val LANGUAGES = listOf("lean", "toml")
    }

    val entityNameProperty = propertyGraph.lazyProperty(::suggestName)
    val locationProperty = propertyGraph.lazyProperty(::suggestLocationByName)
    val fetchingTagsFromGithubProperty = propertyGraph.lazyProperty { false }
    val fetchingTagsError = propertyGraph.lazyProperty { "" }
    val allVersionsProperty = fetchingTagsFromGithubProperty.transform(
        map = {
            getVersions(it)
        },
        backwardMap = {
            throw IllegalStateException("versionProperty should not backward map to fetchingTagsFromGithubProperty")
        }
    )
    val versionProperty = propertyGraph.lazyProperty {
        allVersionsProperty.get()[0]
    }
    val useProxyProperty = propertyGraph.lazyProperty { false }
    val proxyValueProperty = propertyGraph.lazyProperty { "" }
    val canonicalPathProperty = locationProperty.joinCanonicalPath(entityNameProperty)
    val templatesProperty = propertyGraph.property(TEMPLATES.first())
    val languagesProperty = propertyGraph.property(LANGUAGES.first())

    private fun suggestName(): String {
        return suggestName("Untitled")
    }

    fun suggestName(prefix: String): String {
        val projectFileDirectory = File(wizardContext.projectFileDirectory)
        return FileUtil.createSequentFileName(projectFileDirectory, prefix, "")
    }

    private fun suggestLocationByName(): String {
        return wizardContext.projectFileDirectory
    }

    private fun getVersions(fromGithub : Boolean): List<String> {
        val elanService = service<ElanService>()
        if (!fromGithub) {
            return elanService.toolchains(includeRemote = true)
        }

        try {
            val proxy = if (useProxyProperty.get()) {
                val url = URL(proxyValueProperty.get())
                val type = if (url.protocol.contains("sock")) {
                    Proxy.Type.SOCKS
                } else {
                    Proxy.Type.HTTP
                }
                Proxy(type, InetSocketAddress(url.host, url.port))
            } else {
                null
            }
            try {
                return elanService.toolchainsFromGithub(proxy)
            } finally {
                fetchingTagsError.set("")
            }
        } catch (e: ConnectException) {
            fetchingTagsError.set(
                (e.message ?: "unknown error") + ", please consider using proxy<br>fallback to preset versions"
            )
        } catch (e: Exception) {
            fetchingTagsError.set((e.message ?: "unknown error") + "<br>fallback to preset versions")
        }
        return elanService.toolchains(includeRemote = true)
    }

    fun getLocationComment(): String {
        val shortPath = StringUtil.shortenPathWithEllipsis(getPresentablePath(canonicalPathProperty.get()), 60)
        return UIBundle.message(
            "label.project.wizard.new.project.path.description",
            wizardContext.isCreatingNewProjectInt,
            shortPath,
        )
    }

    fun commentForTemplate() = when (templatesProperty.get()) {
        "std" -> "library and executable; default"
        "exe" -> "executable only"
        "lib" -> "library only"
        "math" -> "library only with a mathlib dependency"
        else -> "unrecognized template"
    }

    fun commentForConfigurationLanguage() = when (languagesProperty.get()) {
        "lean" -> "a Lean version of the the configuration file; default"
        "toml" -> "a TOML version of the the configuration file"
        else -> "unrecognized language"
    }

    fun lakeCommandForComment(): String = "Command to create project: ${lakeCommand()}"

    fun lakeCommand(): String {
        val commandBuilder = StringBuilder("lake new ${entityNameProperty.get()}")
        val isDefaultTemplate = templatesProperty.get() == QuickStarterModel.TEMPLATES[0]
        val isDefaultLanguage = languagesProperty.get() == QuickStarterModel.LANGUAGES[0]
        if (!isDefaultTemplate) {
            commandBuilder.append(" ${templatesProperty.get()}")
        }
        if (!isDefaultLanguage) {
            if (isDefaultTemplate) {
                commandBuilder.append(" ")
            }
            commandBuilder.append(".${languagesProperty.get()}")
        }
        return commandBuilder.toString()
    }
}

class QuickStarterPanel(private val model: QuickStarterModel) {

    val root = panel {
        row("template") {
            segmentedButton(QuickStarterModel.TEMPLATES) {
                this.text = it
            }.bind(model.templatesProperty)
            val comment = comment(model.commentForTemplate()).component
            model.templatesProperty.afterChange {
                comment.text = model.commentForTemplate()
            }
        }

        row("language") {
            segmentedButton(QuickStarterModel.LANGUAGES) {
                this.text = it
            }.bind(model.languagesProperty)
            val comment = comment(model.commentForConfigurationLanguage()).component
            model.languagesProperty.afterChange {
                comment.text = model.commentForConfigurationLanguage()
            }
        }
    }
}


class LeanPanel(propertyGraph: PropertyGraph, private val wizardContext: WizardContext, builder: Panel) {

    private val quickStarterModel = QuickStarterModel(propertyGraph, wizardContext)
    private val quickStarterPanel = QuickStarterPanel(quickStarterModel)

    init {
        builder.panel {
            row("Name:") { textField().bindText(quickStarterModel.entityNameProperty) }
            row("Location:") {
                val commentLabel =
                    projectLocationField(quickStarterModel.locationProperty, wizardContext)
                        .align(AlignX.FILL)
                        .comment(quickStarterModel.getLocationComment(), 100)
                        .comment!!
                quickStarterModel.entityNameProperty.afterChange {
                    commentLabel.text = quickStarterModel.getLocationComment()
                    updateModel()
                }
                quickStarterModel.locationProperty.afterChange {
                    commentLabel.text = quickStarterModel.getLocationComment()
                    quickStarterModel.entityNameProperty.set(quickStarterModel.suggestName(quickStarterModel.entityNameProperty.get()))
                    updateModel()
                }
            }
            row("Lean Version") {
                val comboBox = comboBox(quickStarterModel.allVersionsProperty.get())
                quickStarterModel.allVersionsProperty.afterChange {
                    comboBox.component.removeAllItems()
                    quickStarterModel.allVersionsProperty.get().forEach { comboBox.component.addItem(it) }
                }
                comboBox.bindItem(quickStarterModel.versionProperty)
                val comboBoxComponent = comboBox.component
                makeComboBoxSearchable(comboBoxComponent)
                val fetchingTagsFromGithub = checkBox("Fetching tags from Github")
                fetchingTagsFromGithub.bindSelected(quickStarterModel.fetchingTagsFromGithubProperty)
            }
            // add an error message row for downloading lean version, and hide it by default
            row {
                val errorMessage = text("Error message")
                val component = errorMessage.component
                component.isVisible = false
                quickStarterModel.fetchingTagsError.afterChange {
                    if (it.isNotEmpty()) {
                        component.text = it
                        component.foreground = Color.RED
                        component.isVisible = true
                    } else {
                        component.isVisible = false
                    }
                }
            }

            val proxySettingsGroup = collapsibleGroup("Proxy Settings") {
                row("Enable proxy:") {
                    checkBox("").bindSelected(quickStarterModel.useProxyProperty)
                }
                row("Proxy:") {
                    fullWidthCell(
                        textField()
                            .bindText(quickStarterModel.proxyValueProperty)
                            .enabledIf(quickStarterModel.useProxyProperty)
                            .component
                    )
                }.visibleIf(quickStarterModel.useProxyProperty)
                row {
                    comment(
                        """
                        If you are behind a proxy, you can enable this option to download Lean from the internet.<br>
                        The proxy is transformed into an environment variable `HTTP_PROXY` for elan.
                    """.trimIndent()
                    )
                }
            }
            proxySettingsGroup.expanded = false

            val quickStarterGroup = collapsibleGroup("Project Settings") {
                row {
                    cell(quickStarterPanel.root)
                }
            }
            quickStarterGroup.expanded = false
            row {
                val actualCommandComment = comment(quickStarterModel.lakeCommandForComment())
                    .component
                quickStarterModel.entityNameProperty.afterChange {
                    actualCommandComment.text = quickStarterModel.lakeCommandForComment()
                    updateModel()
                }
                quickStarterModel.locationProperty.afterChange {
                    actualCommandComment.text = quickStarterModel.lakeCommandForComment()
                    updateModel()
                }
                quickStarterModel.templatesProperty.afterChange {
                    actualCommandComment.text = quickStarterModel.lakeCommandForComment()
                    updateModel()
                }
                quickStarterModel.languagesProperty.afterChange {
                    actualCommandComment.text = quickStarterModel.lakeCommandForComment()
                    updateModel()
                }
            }
        }
        updateModel()
    }

    private fun updateModel() {
        wizardContext.setProjectFileDirectory(quickStarterModel.canonicalPathProperty.get())
        wizardContext.projectName = quickStarterModel.entityNameProperty.get()
        wizardContext.defaultModuleName = quickStarterModel.entityNameProperty.get()
        wizardContext.getUserData(QUICK_STARTER_MODEL_KEY)?.set(quickStarterModel)
    }

    private fun Row.projectLocationField(
        locationProperty: GraphProperty<String>,
        wizardContext: WizardContext,
    ): Cell<TextFieldWithBrowseButton> {
        val title = IdeBundle.message("title.select.project.file.directory", wizardContext.presentationName)
        val fileChooserDescriptor =
            FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
                .withTitle(title)
                .withFileFilter { it.isDirectory }
                .withPathToTextConvertor(::getPresentablePath)
                .withTextToPathConvertor(::getCanonicalPath)
        val property = locationProperty.transform(::getPresentablePath, ::getCanonicalPath)
        return cell(textFieldWithBrowseButton(wizardContext.project, fileChooserDescriptor).bind(property))
    }

    /**
     * After a long debug I realized that the following code is necessary to make the combo box searchable
     * At first I found the following code with `sdkComboBox` is searchable:
     * ```
     * sdkComboBox(
     *     wizardContext,
     *     sdkProperty,
     *     Lean4SdkType.INSTANCE.name,
     *     { it is Lean4SdkType }
     * )
     * ```
     * which is copied and modifier from intellij-arend, but we don't want sdk here and want to list all versions.
     * Checking https://intellij-support.jetbrains.com/hc/en-us/community/posts/360007069080-ComboBox-Editable-as-Input-field
     * Jakub suggested to check the class [com.intellij.ui.popup.list.ComboBoxPopup] but the class does not give any hints to
     * show how to use it.
     * At last, I put a breakpoint at the constructor of [com.intellij.ui.popup.list.ComboBoxPopup] and it shows
     * [com.intellij.ide.ui.laf.darcula.ui.DarculaJBPopupComboPopup] creates it. And putting a breakpoint at the constructor
     * of [com.intellij.ide.ui.laf.darcula.ui.DarculaJBPopupComboPopup] it shows that
     * [com.intellij.openapi.ui.ComboBox.setSwingPopup] with the argument false creates it.
     * What a hard way to find this!
     */
    private fun makeComboBoxSearchable(comboBoxComponent: ComboBox<String>) {
        comboBoxComponent.isSwingPopup = false
    }
}

class LeanProjectWizardStep(override val context: WizardContext, override val propertyGraph: PropertyGraph) :
    NewProjectWizardStep {

    override val data: UserDataHolder
        get() = UserDataHolderBase()

    override val keywords: NewProjectWizardStep.Keywords
        get() = NewProjectWizardStep.Keywords()

    override fun setupUI(builder: Panel) {
        LeanPanel(propertyGraph, context, builder)
    }

    /**
     * The source code for creating new project originates from vaadin's plugin
     * But it does not override this method.
     *
     * Rather, for me, I have to override this method to set up the project correctly
     * such that the created new project does not occur into problem like this
     * https://intellij-support.jetbrains.com/hc/en-us/community/posts/4527187223826-Project-panel-doesn-t-show-the-folders-and-files-are-looking-as-external
     *
     * The following code is from [com.intellij.ide.wizard.language.EmptyProjectGeneratorNewProjectWizard.Step.setupProject]
     * The moduleTypeID here must be [GeneralModuleType.TYPE_ID]
     */
    override fun setupProject(project: Project) {
        val moduleType = ModuleTypeManager.getInstance().findByID(GeneralModuleType.TYPE_ID)
        val builder = moduleType.createModuleBuilder()
        val model = context.getUserData(NewProjectWizardStep.MODIFIABLE_MODULE_MODEL_KEY)
        builder.commit(project, model)
    }
}

class LeanProjectWizard : GeneratorNewProjectWizard {
    override val icon: Icon
        get() = Lean4Icons.FILE
    override val id: String
        get() = "Lean4"
    override val name: String
        get() = "Lean4"

    private val propertyGraph: PropertyGraph
        get() = PropertyGraph("Lean4 project")

    private val quickStarterModelProperty = propertyGraph.property<QuickStarterModel?>(null)

    val quickStarterModel: QuickStarterModel? by quickStarterModelProperty

    override fun createStep(context: WizardContext): NewProjectWizardStep {
        context.putUserData(QUICK_STARTER_MODEL_KEY, quickStarterModelProperty)
        return LeanProjectWizardStep(context, propertyGraph)
    }
}

class Lean4ModuleBuilder(private val leanWizard: LeanProjectWizard = LeanProjectWizard()) :
    GeneratorNewProjectWizardBuilderAdapter(leanWizard) {

    override fun createProject(name: String, path: String): Project? {
        return super.createProject(name, path)?.let { project ->
            val quickStarterModel = leanWizard.quickStarterModel!!
            // TODO for some region here it requires proxy
            // TODO version not support yet and
            //      although from https://leanprover.zulipchat.com/#narrow/channel/113489-new-members/topic/lake.20new.20project.20with.20specified.20lean.20version.3F
            //      we know lake with elan can automatically download lean
            //      but we may manually do it with a update etc
            val elanService = service<ElanService>()
            val command = "${elanService.elanBinPath}${File.separatorChar}${quickStarterModel.lakeCommand()}"
            val envs = if (quickStarterModel.useProxyProperty.get()) {
                mapOf()
            } else {
                mapOf("HTTPS_PROXY" to quickStarterModel.proxyValueProperty.get())
            }
            // TODO running this command directly here may block the ui
            val result = command.execute(File(quickStarterModel.locationProperty.get()), envs)

            // TODO could this really be null in our case?
            project.basePath?.let { basePath ->
                val version = quickStarterModel.versionProperty.get()
                // TODO this and some other code in this method/file etc should be refactored into ElanService
                Path.of(basePath, "lean-toolchain").toFile().writeText("leanprover/lean4:${version}")
            }

            // TODO DRY with lean4ij.project.LeanProjectActivity.addRunConfigurations
            val runManager = RunManager.getInstance(project)
            val elanRunConfigurationType =
                ConfigurationTypeUtil.findConfigurationType(ElanRunConfigurationType::class.java)
            val configuration =
                runManager.createConfiguration("elan which lean", elanRunConfigurationType.configurationFactories[0])
            val elanRuhConfiguration = configuration.configuration as ElanRunConfiguration
            // TODO modify lean-toolchain file to the specified version

            elanRuhConfiguration.options.arguments = "which lean"
            if (quickStarterModel.useProxyProperty.get()) {
                elanRuhConfiguration.options.environments["HTTPS_PROXY"] =
                    quickStarterModel.proxyValueProperty.get().trim()
            }
            runManager.addConfiguration(configuration)

            // The configuration cannot be executed directly here. It's too early, the except before using invokeLater:
            //     You must not register toolwindow programmatically so early. Rework code or use ToolWindowManager.invokeLater
            //     java.lang.IllegalStateException: You must not register toolwindow programmatically so early. Rework code or use ToolWindowManager.invokeLater
            //     at com.intellij.openapi.wm.impl.ToolWindowManagerImpl.getDefaultToolWindowPaneIfInitialized(ToolWindowManagerImpl.kt:584)
            val leanProject = project.service<LeanProjectService>()
            leanProject.isProjectCreating = true
            ToolWindowManager.getInstance(project).invokeLater {
                // check https://plugins.jetbrains.com/docs/intellij/run-configurations.html#starting-a-run-configuration-programmatically
                // for Starting a Run Configuration Programmatically
                ProgramRunnerUtil.executeConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance())
                // Here we set up the sdk by
                // skipping some in lean4ij.project.LeanProjectActivity.execute
                project.service<SdkService>().setupModule()
                leanProject.isProjectCreating = false
            }

            project
        }
    }
}
