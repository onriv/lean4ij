package lean4ij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.util.IconLoader
import lean4ij.infoview.LeanInfoviewService
import javax.swing.Icon

/**
 * TODO maybe add some buttons/actions for stop/start/toggle automatically refreshing the infoview
 *      which turn it into a manual mode
 */
class ToggleAutomaticallyRefreshInternalInfoview : AnAction() {

    private val onIcon: Icon = IconLoader.getIcon("/icons/textAutoGenerate.svg", javaClass)
    private val offIcon: Icon = AllIcons.Actions.Suspend
    init {
        templatePresentation.icon = onIcon
        templatePresentation.disabledIcon = offIcon
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project?:return
        val leanInfoviewService = project.service<LeanInfoviewService>()
        leanInfoviewService.automaticallyRefreshInternalInfoview = !leanInfoviewService.automaticallyRefreshInternalInfoview
        if (!leanInfoviewService.automaticallyRefreshInternalInfoview) {
            e.presentation.isEnabled = false
        } else {
            e.presentation.isEnabled = true
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}