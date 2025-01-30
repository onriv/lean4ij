package lean4ij.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import lean4ij.language.Lean4Icons
import javax.swing.JComponent
import javax.swing.JLabel


class Lean4ModuleBuilder : ModuleBuilder() {
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
