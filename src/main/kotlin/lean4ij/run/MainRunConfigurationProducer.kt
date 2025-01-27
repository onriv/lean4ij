package lean4ij.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import lean4ij.language.Lean4Definition
import lean4ij.language.psi.TokenType
import kotlin.io.path.relativeTo

class MainRunConfigurationProducer : LazyRunConfigurationProducer<LeanRunConfiguration>() {

    companion object {
        private val LEAN_RUN_CONFIGURATION_FACTORY = LeanConfigurationFactory(LeanRunConfigurationType())
    }

    override fun getConfigurationFactory(): ConfigurationFactory =
        LEAN_RUN_CONFIGURATION_FACTORY

    override fun setupConfigurationFromContext(
        configuration: LeanRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val intermediateConfiguration = getConfiguration(context, sourceElement) ?: return false
        configuration.name = intermediateConfiguration.name
        configuration.options.fileName = intermediateConfiguration.fileName
        configuration.options.arguments = intermediateConfiguration.arguments
        return true
    }

    private fun getConfiguration(context: ConfigurationContext, sourceElement: Ref<PsiElement>?): IntermediateLeanRunConfiguration? {
        val element = context.location?.psiElement?:return  null
        if (element.parent !is Lean4Definition) return null
        if (element.node.elementType == TokenType.IDENTIFIER && element.node.text == "main") {
            if (element.nextSibling == null && element.prevSibling.elementType == TokenType.WHITE_SPACE) {
                val projectDir = context.project.guessProjectDir()?:return null
                val file = element.containingFile.virtualFile
                val name = file.toNioPath().relativeTo(projectDir.toNioPath()).toString()
                return IntermediateLeanRunConfiguration(name, file.toNioPath().toString(), "")
            }
        }
        return null
    }

    override fun isConfigurationFromContext(
        configuration: LeanRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val intermediateConfiguration = getConfiguration(context, null) ?: return false
        return intermediateConfiguration.arguments ==  configuration.options.arguments &&
                intermediateConfiguration.fileName == configuration.options.fileName
    }

    private data class IntermediateLeanRunConfiguration(
        val name: String,
        val fileName: String,
        val arguments: String
    )
}