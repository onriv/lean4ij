package lean4ij.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.util.IconLoader
import lean4ij.infoview.LeanInfoviewService

class ToggleLeanInfoviewToolbarVisibility : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    init {
        templatePresentation.icon = IconLoader.getIcon("/icons/review_eye.svg", javaClass);
    }

    /**
     * TODO make a service for the Internal infoview
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val actionToolbar = project.service<LeanInfoviewService>().actionToolbar ?: return
        val component = actionToolbar.component
        component.isVisible = !component.isVisible
    }

}