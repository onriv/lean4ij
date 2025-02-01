package lean4ij.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import lean4ij.language.Lean4Icons
import javax.swing.JComponent
import javax.swing.JLabel
import com.intellij.ui.UIBundle
import com.intellij.ui.dsl.builder.*
import javax.swing.JTextField


fun <T : JComponent> Panel.aligned(text: String, component: T, init: Cell<T>.() -> Unit = {}) = row(text) {
    cell(component).align(AlignX.FILL).init()
}

class Lean4ModuleBuilder : ModuleBuilder() {

    private val propertyGraph: PropertyGraph = PropertyGraph()
    private val projectNameProperty: GraphProperty<String> = propertyGraph.lazyProperty (::untitledName)
    private val locationProperty: GraphProperty<String> = propertyGraph.lazyProperty(::defaultLocation)

    private fun untitledName() : String = "Untitled"

    // TODO define default location
    private fun defaultLocation() : String = "TODO"

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


    override fun getCustomOptionsStep(context: WizardContext?, parentDisposable: Disposable?): ModuleWizardStep {
        return object: ModuleWizardStep() {
            override fun getComponent(): JComponent {
                return panel {
                    row(UIBundle.message("label.project.wizard.new.project.name")) {
                        textField().bindText(projectNameProperty)
                            .columns(COLUMNS_MEDIUM)
                            .gap(RightGap.SMALL)
                            .focused()
                    }.bottomGap(BottomGap.SMALL)
                    row(UIBundle.message("label.project.wizard.new.project.location")) {

                    }
                }
            }

            override fun updateDataModel() {

            }

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
