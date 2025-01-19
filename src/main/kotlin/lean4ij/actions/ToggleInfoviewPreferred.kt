package lean4ij.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
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
            lean4Settings.state.preferredInfoview == "Jcef" ->
                    toggleLeanInfoViewJcef.actionPerformed(e)
            else ->
                    toggleLeanInfoViewInternal.actionPerformed(e)
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.dataContext.getData(CommonDataKeys.EDITOR)?:return
        // TODO here it can be null, weird
        val virtualFile = editor.virtualFile?: return
        if (!LeanUtil.isLeanFile(virtualFile)) {
            e.presentation.isVisible = false
        }
    }
}