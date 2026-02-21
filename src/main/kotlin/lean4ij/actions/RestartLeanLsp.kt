package lean4ij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import lean4ij.project.LeanProjectService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

/**
 * Action to manually restart the Lean Language Server.
 *
 * This is useful if the server becomes unresponsive or gets into a bad state.
 * It triggers a full stop and start of the LSP process via [LeanProjectService].
 */
class RestartLeanLsp : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    init {
        templatePresentation.icon = AllIcons.Actions.StopRefresh
        templatePresentation.text = "Restart Lean LSP Server"
        templatePresentation.description = "Restarts the Lean 4 Language Server process."
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            val leanProjectService: LeanProjectService = it.service()
            leanProjectService.restartLsp()
        }
    }

}

