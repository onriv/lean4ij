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

import com.intellij.ide.IdeBundle
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.joinCanonicalPath
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withPathToTextConvertor
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withTextToPathConvertor
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.getCanonicalPath
import com.intellij.openapi.ui.getPresentablePath
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.UIBundle
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.CollapsibleRow
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import lean4ij.language.Lean4Icons
import lean4ij.project.ElanService
import lean4ij.util.runCommand
import java.io.File
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JComponent

val QUICK_STARTER_MODEL_KEY = Key<GraphProperty<QuickStarterModel?>>("lean4_quick_starter_model")

fun <T : JComponent> Panel.aligned(text: String, component: T, init: Cell<T>.() -> Unit = {}) = row(text) {
    cell(component).align(AlignX.FILL).init()
}

class QuickStarterModel(private val propertyGraph: PropertyGraph, private val wizardContext: WizardContext) : BaseState() {

    companion object {
        val TEMPLATES = listOf("std", "exe", "lib", "math")
        val LANGUAGES = listOf("lean", "toml")
    }

    val entityNameProperty = propertyGraph.lazyProperty(::suggestName)
    val locationProperty = propertyGraph.lazyProperty(::suggestLocationByName)
    val canonicalPathProperty = locationProperty.joinCanonicalPath(entityNameProperty)
    val templatesProperty = propertyGraph.property(TEMPLATES.first())
    val languagesProperty = propertyGraph.property(LANGUAGES.first())
    // val useAuthenticationProperty = graph.property(false)
    // val usePrereleaseProperty = graph.property(false)

    private val templates by templatesProperty
    private val languages by languagesProperty
    // private val useAuthentication by useAuthenticationProperty
    // private val usePrerelease by usePrereleaseProperty

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
            ".lean" -> "a Lean version of the the configuration file; default"
            ".toml" -> "a TOML version of the the configuration file"
            else -> "unrecognized language"
        }

    fun lakeCommandForComment(): String = "Command to create project: ${lakeCommand()}"

    fun lakeCommand() : String {
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

    private val sdkProperty: GraphProperty<Sdk?> = propertyGraph.property(null )

    private var quickStarterGroup: CollapsibleRow? = null

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
            addSdkUi(wizardContext)

            quickStarterGroup = collapsibleGroup("Project Settings") {
                row {
                    cell(quickStarterPanel.root)
                }
            }
            quickStarterGroup!!.expanded = false
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

        quickStarterGroup!!.expanded = true
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
        val fileChooserDescriptor =
            FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
                .withFileFilter { it.isDirectory }
                .withPathToTextConvertor(::getPresentablePath)
                .withTextToPathConvertor(::getCanonicalPath)
        val title = IdeBundle.message("title.select.project.file.directory", wizardContext.presentationName)
        val property = locationProperty.transform(::getPresentablePath, ::getCanonicalPath)
        return textFieldWithBrowseButton(title, wizardContext.project, fileChooserDescriptor).bindText(property)
    }

    private fun Panel.addSdkUi(context: WizardContext) {
        row("Lean Version") {
            val comboBoxComponent = comboBox(service<ElanService>().toolchains(includeRemote = true)).component
            // // after a long debug, I realized that after calling
            makeComboBoxSearchable(comboBoxComponent)
            comboBoxComponent.isSwingPopup = false
        }
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

class LeanProjectWizardStep (override val context: WizardContext, override val propertyGraph: PropertyGraph) :
    NewProjectWizardStep {

    override val data: UserDataHolder
        get() = UserDataHolderBase()

    override val keywords: NewProjectWizardStep.Keywords
        get() = NewProjectWizardStep.Keywords()

    override fun setupUI(builder: Panel) {
        LeanPanel(propertyGraph, context, builder)
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

class Lean4ModuleBuilder(private val leanWizard: LeanProjectWizard = LeanProjectWizard()):
    GeneratorNewProjectWizardBuilderAdapter(leanWizard) {

    private val propertyGraph = PropertyGraph()

    override fun createStep(context: WizardContext): NewProjectWizardStep {
        return leanWizard.createStep(context)
    }

    override fun createProject(name: String?, path: String?): Project? {
        return super.createProject(name, path)?.let { project ->
            val quickStarterModel = leanWizard.quickStarterModel!!
            "${Path.of(System.getProperty("user.home"), ".elan", "bin")}${File.separatorChar}${quickStarterModel.lakeCommand()}".runCommand(File(quickStarterModel.locationProperty.get()))
            // "lake new $name".runCommand(File(path))
            project
        }
    }

    override fun isAvailable(): Boolean {
        val lastPerformedActionId = (ActionManager.getInstance() as ActionManagerImpl).lastPreformedActionId
        lastPerformedActionId ?: return false
        return lastPerformedActionId.contains("NewProject", true)
    }

    private fun afterProjectCreated(project: Project) {
        VfsUtil.findFileByIoFile(File(project.basePath, "README.md"), true)?.let {
            val descriptor = OpenFileDescriptor(project, it)
            descriptor.setUsePreviewTab(true)
            FileEditorManager.getInstance(project).openEditor(descriptor, true)
        }
    }
}


class Lean4ModuleType : ModuleType<Lean4ModuleBuilder>("LEAN4_MODULE") {
    override fun getNodeIcon(isOpened: Boolean) = Lean4Icons.FILE

    override fun createModuleBuilder() = Lean4ModuleBuilder()

    override fun getDescription() = "Lean4 library"

    override fun getName() = "Lean4"

    companion object {
        fun has(module: Module?) = module != null && `is`(module, INSTANCE)

        @JvmField
        val INSTANCE = Lean4ModuleType()
    }
}

// class LeanModuleWizardStep(override val context: WizardContext, override val propertyGraph: PropertyGraph) : NewProjectWizardStep {
//
//     override val data: UserDataHolder
//         get() = TODO("Not yet implemented")
//     override val keywords: NewProjectWizardStep.Keywords
//         get() = TODO("Not yet implemented")
//
//     private val projectNameProperty: GraphProperty<String> = propertyGraph.lazyProperty(this::suggestName)
//     private val locationProperty: GraphProperty<String> = propertyGraph.lazyProperty(::defaultLocation)
//     private val canonicalPathProperty = locationProperty.joinCanonicalPath(projectNameProperty)
//     private val gitProperty: GraphProperty<Boolean> = propertyGraph.property(false)
//         .bindBooleanStorage(NewProjectWizardStep.GIT_PROPERTY_NAME)
//
//     private val sdkModel: ProjectSdksModel = ProjectSdksModel()
//     private val sdkProperty: GraphProperty<Sdk?> = propertyGraph.property(null )
//
//     override fun getComponent(): JComponent {
//         return panel {
//             row(UIBundle.message("label.project.wizard.new.project.name")) {
//                 textField().bindText(projectNameProperty)
//                     .columns(COLUMNS_MEDIUM)
//                     .gap(RightGap.SMALL)
//                     .focused()
//             }.bottomGap(BottomGap.SMALL)
//             val locationRow = row(UIBundle.message("label.project.wizard.new.project.location")) {
//                 projectLocationField(locationProperty, context!!)
//                     .align(AlignX.FILL)
//                     .comment(getLocationComment(context), 100)
//             }
//             if (context.isCreatingNewProject) {
//                 // Git should not be enabled for single module
//                 row("") {
//                     checkBox(UIBundle.message("label.project.wizard.new.project.git.checkbox"))
//                         .bindSelected(gitProperty)
//                 }.bottomGap(BottomGap.SMALL)
//             } else {
//                 locationRow.bottomGap(BottomGap.SMALL)
//             }
//             addSdkUi(context)
//         }.withVisualPadding(topField = true)
//     }
//
//     override fun updateDataModel() {
//         val lake = service<ElanService>().getDefaultLakePath()
//         "$lake new ${projectNameProperty.get()}".runCommand(File(locationProperty.get()))
//     }
//
//     /**
//      * From intellij-arend, TODO add concrete link
//      * This is a literal copy... make sure it does not violate the license...
//      */
//     private fun Row.projectLocationField(
//         locationProperty: GraphProperty<String>,
//         wizardContext: WizardContext
//     ): Cell<TextFieldWithBrowseButton> {
//         val fileChooserDescriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
//             .withFileFilter { it.isDirectory }
//             .withPathToTextConvertor(::getPresentablePath)
//             .withTextToPathConvertor(::getCanonicalPath)
//         val title = IdeBundle.message("title.select.project.file.directory", wizardContext.presentationName)
//         val property = locationProperty.transform(::getPresentablePath, ::getCanonicalPath)
//         return textFieldWithBrowseButton(
//             null, wizardContext.project,
//             fileChooserDescriptor.withTitle(title)
//         )
//             .bindText(property)
//     }
//
//     /**
//      * From intellij-arend, and from the following link we know that we must depend on the java plugin
//      * https://intellij-support.jetbrains.com/hc/en-us/community/posts/8536219607570-How-do-I-create-a-SDK-selector-ComboBox-in-an-IntelliJ-plugin
//      */
//     private fun Panel.addSdkUi(context: WizardContext) {
//         row("Lean Version") {
//             sdkComboBox(context, sdkProperty, Lean4SdkType.INSTANCE.name, {it is Lean4SdkType})
//                 .columns(COLUMNS_MEDIUM)
//                 .component
//         }
//         row {
//             comment("Project SDK is needed if you want to create a language extension or debug typechecking")
//         }.bottomGap(BottomGap.SMALL)
//     }
//
//     private fun getLocationComment(context: WizardContext): String {
//         val shortPath = StringUtil.shortenPathWithEllipsis(getPresentablePath(canonicalPathProperty.get()), 60)
//         return UIBundle.message(
//             "label.project.wizard.new.project.path.description",
//             context.isCreatingNewProjectInt,
//             shortPath
//         )
//     }
//
//     /**
//      * This is copied from intellij-arend
//      */
//     private fun suggestName(): String = suggestName(DEFAULT_MODULE_ARTIFACT)
//
//     private fun suggestName(prefix: String): String {
//         val projectFileDirectory = File(context.projectFileDirectory)
//         return FileUtil.createSequentFileName(projectFileDirectory, prefix, "")
//     }
//
//     // TODO define default location
//     private fun defaultLocation(): String = context.projectFileDirectory
//
// }