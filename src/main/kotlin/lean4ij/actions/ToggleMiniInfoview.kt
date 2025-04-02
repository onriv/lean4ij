package lean4ij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import kotlinx.coroutines.launch
import lean4ij.infoview.MiniInfoviewService
import lean4ij.setting.Lean4Settings

class ToggleMiniInfoview : AnAction() {


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    init {
        templatePresentation.icon = AllIcons.Actions.Minimap
    }

    override fun update(e: AnActionEvent) {
        e.presentation.setEnabledAndVisible(e.getData(CommonDataKeys.EDITOR) != null);
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.service<MiniInfoviewService>()
        val lean4Settings = service<Lean4Settings>()
        if (lean4Settings.enableNativeInfoview) {
            service.scope.launch {
                service.toggleVisibility()
            }
        }
    }
}
