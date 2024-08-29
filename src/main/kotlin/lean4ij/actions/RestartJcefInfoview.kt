package lean4ij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import lean4ij.infoview.external.JcefInfoviewService

class RestartJcefInfoview : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<JcefInfoviewService>()?.reload()
    }
}