package com.github.onriv.ijpluginlean.language

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType

class Lean4Context : TemplateContextType("Lean4") {

    override fun isInContext(templateActionContext: TemplateActionContext) : Boolean{
        // TODO configurable?
        return templateActionContext.getFile().getName().endsWith(".lean")
                || templateActionContext.getFile().getName().endsWith(".lean4");
    }

}
