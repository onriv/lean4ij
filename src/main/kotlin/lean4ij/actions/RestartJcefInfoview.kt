package lean4ij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.IconLoader.getIcon
import lean4ij.infoview.LeanInfoviewService
import lean4ij.infoview.external.JcefInfoviewService
import lean4ij.project.LeanProjectService
import javax.swing.Icon

class RestartJcefInfoview : AnAction() {

    init {
        templatePresentation.icon = AllIcons.Actions.Restart
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<JcefInfoviewService>()?.reload()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}

class RestartInternalInfoview : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    init {
        templatePresentation.icon = AllIcons.Actions.Restart
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project?:return
        FileEditorManager.getInstance(project).selectedTextEditor?.let { editor ->
            // TODO here in fact message up two things:
            //      one is recreating a new editor for removing old editor's bug
            //      the other is updating infoview manually
            project.service<LeanInfoviewService>().toolWindow?.restartEditor()
            project.service<LeanProjectService>().updateInfoviewFor(editor, true)
        }
    }
}

/**
 * TODO maybe add some buttons/actions for stop/start/toggle automatically refreshing the infoview
 *      which turn it into a manual mode
 */
class ToggleAutomaticallyRefreshInternalInfoview : AnAction() {

    private val onIcon: Icon = getIcon("/icons/textAutoGenerate.svg", javaClass)
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