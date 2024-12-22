package lean4ij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import lean4ij.infoview.LeanInfoviewService
import lean4ij.project.LeanProjectService

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