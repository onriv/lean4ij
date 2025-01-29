package lean4ij.language

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.openapi.components.service
import lean4ij.sdk.LeanLibrary
import lean4ij.setting.Lean4Settings

abstract class BaseLean4TemplateContext(presentableName : String) : TemplateContextType(presentableName) {
    val settings = service<Lean4Settings>()
    override fun isInContext(templateActionContext: TemplateActionContext) : Boolean{
        // TODO configurable?
        val fileName = templateActionContext.getFile().getName()
        val isLeanFile = fileName.endsWith(".lean")
                || fileName.endsWith(".lean4")
        return isLeanFile && isInLeanContext(templateActionContext)
    }

    abstract fun isInLeanContext(templateActionContext: TemplateActionContext): Boolean
}

class Lean4Context : BaseLean4TemplateContext("Lean4") {
    override fun isInLeanContext(templateActionContext: TemplateActionContext): Boolean {
        return !settings.addSpaceAfterLiveTemplates || settings.enableBothSpaceAndNonSpaceLiveTemplates
    }

}

class Lean4SpaceContext : BaseLean4TemplateContext("Lean4Space") {
    override fun isInLeanContext(templateActionContext: TemplateActionContext): Boolean {
        return settings.addSpaceAfterLiveTemplates || settings.enableBothSpaceAndNonSpaceLiveTemplates
    }

}

class Lean4PairContext : BaseLean4TemplateContext("Lean4Pair") {
    override fun isInLeanContext(templateActionContext: TemplateActionContext): Boolean {
        return settings.autoCompletePairLiveTemplates
    }

}
