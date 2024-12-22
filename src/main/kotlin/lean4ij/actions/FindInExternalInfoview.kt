package lean4ij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import kotlinx.coroutines.launch
import lean4ij.infoview.external.JcefInfoviewService
import lean4ij.util.leanProjectScope

class FindInExternalInfoview : AnAction() {

    init {
        templatePresentation.icon = AllIcons.Actions.Find
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val jcefInfoviewService = project.service<JcefInfoviewService>()
        jcefInfoviewService.searchTextField.isVisible = !jcefInfoviewService.searchTextField.isVisible
        if (!jcefInfoviewService.searchTextField.isVisible) {
            project.leanProjectScope.launch {
                jcefInfoviewService.searchTextFlow.send("")
            }
            return
        }
        project.leanProjectScope.launch {
            jcefInfoviewService.searchTextFlow.send(jcefInfoviewService.searchTextField.text)
        }

    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT;
    }
}