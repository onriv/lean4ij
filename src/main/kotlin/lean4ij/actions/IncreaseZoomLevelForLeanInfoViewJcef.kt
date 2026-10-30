package lean4ij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import lean4ij.infoview.external.JcefSupport
import lean4ij.infoview.external.JcefInfoviewService

class IncreaseZoomLevelForLeanInfoViewJcef : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    init {
        templatePresentation.icon = AllIcons.Graph.ZoomIn
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = JcefSupport.isSupported
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (!JcefSupport.isSupported) return
        val project = e.project ?: return
        val service = project.service<JcefInfoviewService>()
        service.increaseZoomLevel()
    }
}