package com.github.onriv.ijpluginlean.actions

import com.github.onriv.ijpluginlean.project.LeanProjectService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class RestartLeanLsp : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            val leanProjectService: LeanProjectService = it.service()
            leanProjectService.restartLsp()
        }
    }

}
