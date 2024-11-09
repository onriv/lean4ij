package lean4ij.actions

import com.intellij.icons.AllIcons
import lean4ij.project.LeanProjectService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import kotlinx.coroutines.launch

class RestartCurrentFile : AnAction() {

    init {
        templatePresentation.icon = AllIcons.Actions.RestartFrame
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {project ->
            val leanProjectService : LeanProjectService = project.service()
            FileEditorManager.getInstance(project).selectedTextEditor?.let { editor ->
                val file = editor.virtualFile.path
                leanProjectService.scope.launch {
                    leanProjectService.file(file).restart()
                }
            }
        }
    }
}