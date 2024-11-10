package lean4ij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import lean4ij.infoview.external.JcefInfoviewService
import lean4ij.project.LeanProjectService
import lean4ij.util.LeanUtil

class RestartJcefInfoview : AnAction() {

    init {
        templatePresentation.icon = AllIcons.Actions.Restart
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<JcefInfoviewService>()?.reload()
    }
}

class RestartInternalInfoview : AnAction() {

    init {
        templatePresentation.icon = AllIcons.Actions.Restart
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project?:return
        FileEditorManager.getInstance(project).selectedTextEditor?.let { editor ->
            project.service<LeanProjectService>().updateInfoviewFor(editor, true)
        }
    }
}
