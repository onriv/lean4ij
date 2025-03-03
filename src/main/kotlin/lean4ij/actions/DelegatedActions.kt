package lean4ij.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class DelegatedAction(private val id: String) : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        ActionManager.getInstance().getAction(id).actionPerformed(e)
    }
}

/**
 * This is unused for plugin.xml already support adding an external action
 * to a customized group with the action id
 */
class LeanGotoDeclaration : DelegatedAction("LSP.GotoDeclaration")
class LeanGotoTypeDefinition : DelegatedAction("LSP.GotoTypeDefinition")
class LeanGotoImplementation : DelegatedAction("LSP.GotoImplementation")
class LeanGotoReference : DelegatedAction("LSP.GotoReference")
