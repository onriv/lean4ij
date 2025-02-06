package lean4ij.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import lean4ij.infoview.LeanInfoviewService
import lean4ij.project.LeanProjectService
import lean4ij.setting.Lean4Settings
import lean4ij.util.LeanUtil

class ToggleInfoviewPreferred : AnAction() {
    private val lean4Settings = service<Lean4Settings>()
    // TODO constant for the actions
    private val toggleLeanInfoViewInternal = ActionManager.getInstance().getAction("ToggleLeanInfoViewInternal")
    private val toggleLeanInfoViewJcef = ActionManager.getInstance().getAction("ToggleLeanInfoViewJcef")

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT;
    }

    override fun actionPerformed(e: AnActionEvent) {
        when {
            lean4Settings.preferredInfoview == "Jcef" ->
                    toggleLeanInfoViewJcef.actionPerformed(e)
            else ->
                    toggleLeanInfoViewInternal.actionPerformed(e)
        }
    }

    /**
     * Very strict visibility check, only visible when the current file is a Lean file
     * only in a lean project and only focusing the editor
     */
    private fun isVisible(e: AnActionEvent) : Boolean {
        val project = e.project?:return false
        val leanProjectService = project.service<LeanProjectService>()
        if (!leanProjectService.isLeanProject()) {
            return false
        }
        val editor = e.dataContext.getData(CommonDataKeys.EDITOR)?:return false
        val virtualFile = editor.virtualFile?:return false
        return LeanUtil.isLeanFile(virtualFile)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = isVisible(e)
    }
}