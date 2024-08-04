package com.github.onriv.ijpluginlean.actions

import com.github.onriv.ijpluginlean.project.LeanProjectService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import kotlinx.coroutines.launch

class RestartCurrentFile : AnAction() {

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