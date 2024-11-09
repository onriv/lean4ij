package lean4ij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import lean4ij.infoview.external.JcefInfoviewService

class RestartJcefInfoview : AnAction() {

    init {
        templatePresentation.icon = AllIcons.Actions.Restart
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<JcefInfoviewService>()?.reload()
    }
}