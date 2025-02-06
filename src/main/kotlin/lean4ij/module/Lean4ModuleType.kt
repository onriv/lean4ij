package lean4ij.module

import com.intellij.ide.IdeBundle
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.withVisualPadding
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.observable.util.joinCanonicalPath
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.roots.ui.configuration.sdkComboBox
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withPathToTextConvertor
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withTextToPathConvertor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.getCanonicalPath
import com.intellij.openapi.ui.getPresentablePath
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.UIBundle
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import lean4ij.language.Lean4Icons
import lean4ij.language.Lean4SdkType
import javax.swing.JComponent
import javax.swing.JLabel


fun <T : JComponent> Panel.aligned(text: String, component: T, init: Cell<T>.() -> Unit = {}) = row(text) {
    cell(component).align(AlignX.FILL).init()
}

class Lean4ModuleBuilder : ModuleBuilder() {

    private val propertyGraph: PropertyGraph = PropertyGraph()
    private val projectNameProperty: GraphProperty<String> = propertyGraph.lazyProperty(::untitledName)
    private val locationProperty: GraphProperty<String> = propertyGraph.lazyProperty(::defaultLocation)
    private val canonicalPathProperty = locationProperty.joinCanonicalPath(projectNameProperty)
    private val gitProperty: GraphProperty<Boolean> = propertyGraph.property(false)
        .bindBooleanStorage(NewProjectWizardStep.GIT_PROPERTY_NAME)

    private val sdkModel: ProjectSdksModel = ProjectSdksModel()
    private val sdkProperty: GraphProperty<Sdk?> = propertyGraph.property(null )

    private fun untitledName(): String = "Untitled"

    // TODO define default location
    private fun defaultLocation(): String = "TODO"

    override fun getModuleType() = Lean4ModuleType.INSTANCE

    /**
     * ref: https://plugins.jetbrains.com/docs/intellij/project-wizard.html#implementing-module-builder
     * TODO the doc says this is mandatory to implement
     */
    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        super.setupRootModel(modifiableRootModel)
    }

    override fun createWizardSteps(
        wizardContext: WizardContext,
        modulesProvider: ModulesProvider
    ): Array<ModuleWizardStep> {
        return arrayOf(object : ModuleWizardStep() {
            override fun getComponent(): JComponent {
                return JLabel("Put your content here")
            }

            override fun updateDataModel() {
            }
        })
    }

    override fun isSuitableSdkType(sdkType: SdkTypeId?): Boolean {
        return true
    }

    override fun createProject(name: String?, path: String?): Project? {
        return super.createProject(name, path)
    }


    /**
     * This is shown in the generator sections. I suspect that only official plugins can be shown in the "New Project" section
     */
    override fun getCustomOptionsStep(context: WizardContext?, parentDisposable: Disposable?): ModuleWizardStep {
        return object : ModuleWizardStep() {
            override fun getComponent(): JComponent {
                return panel {
                    row(UIBundle.message("label.project.wizard.new.project.name")) {
                        textField().bindText(projectNameProperty)
                            .columns(COLUMNS_MEDIUM)
                            .gap(RightGap.SMALL)
                            .focused()
                    }.bottomGap(BottomGap.SMALL)
                    val locationRow = row(UIBundle.message("label.project.wizard.new.project.location")) {
                        projectLocationField(locationProperty, context!!)
                            .align(AlignX.FILL)
                            .comment(getLocationComment(context), 100)
                    }
                    if (context!!.isCreatingNewProject) {
                        // Git should not be enabled for single module
                        row("") {
                            checkBox(UIBundle.message("label.project.wizard.new.project.git.checkbox"))
                                .bindSelected(gitProperty)
                        }.bottomGap(BottomGap.SMALL)
                    } else {
                        locationRow.bottomGap(BottomGap.SMALL)
                    }
                    addSdkUi(context)
                }.withVisualPadding(topField = true)
            }

            override fun updateDataModel() {

            }

        }
    }


    /**
     * From intellij-arend, TODO add concrete link
     * This is a literal copy... make sure it does not violate the license...
     */
    private fun Row.projectLocationField(
        locationProperty: GraphProperty<String>,
        wizardContext: WizardContext
    ): Cell<TextFieldWithBrowseButton> {
        val fileChooserDescriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
            .withFileFilter { it.isDirectory }
            .withPathToTextConvertor(::getPresentablePath)
            .withTextToPathConvertor(::getCanonicalPath)
        val title = IdeBundle.message("title.select.project.file.directory", wizardContext.presentationName)
        val property = locationProperty.transform(::getPresentablePath, ::getCanonicalPath)
        return textFieldWithBrowseButton(
            null, wizardContext.project,
            fileChooserDescriptor.withTitle(title)
        )
            .bindText(property)
    }

    /**
     * From intellij-arend, and from the following link we know that we must depend on the java plugin
     * https://intellij-support.jetbrains.com/hc/en-us/community/posts/8536219607570-How-do-I-create-a-SDK-selector-ComboBox-in-an-IntelliJ-plugin
     */
    private fun Panel.addSdkUi(context: WizardContext) {
        row("Lean Version") {
            sdkComboBox(context, sdkProperty, Lean4SdkType.INSTANCE.name, {it is Lean4SdkType})
                .columns(COLUMNS_MEDIUM)
                .component
        }
        row {
            comment("Project SDK is needed if you want to create a language extension or debug typechecking")
        }.bottomGap(BottomGap.SMALL)
    }

    private fun getLocationComment(context: WizardContext): String {
        val shortPath = StringUtil.shortenPathWithEllipsis(getPresentablePath(canonicalPathProperty.get()), 60)
        return UIBundle.message(
            "label.project.wizard.new.project.path.description",
            context.isCreatingNewProjectInt,
            shortPath
        )
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