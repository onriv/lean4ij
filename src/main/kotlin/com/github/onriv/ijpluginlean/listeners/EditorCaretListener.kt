package com.github.onriv.ijpluginlean.listeners

import com.github.onriv.ijpluginlean.lsp.LeanLspServerManager
import com.github.onriv.ijpluginlean.lsp.data.Position
import com.github.onriv.ijpluginlean.services.CursorLocation
import com.github.onriv.ijpluginlean.services.ExternalInfoViewService
import com.github.onriv.ijpluginlean.services.Range
import com.github.onriv.ijpluginlean.toolWindow.LeanInfoViewWindowFactory
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

class EditorCaretListener(val project: Project) : CaretListener {

    companion object {

        private var registered = false

        @Synchronized
        fun register(project: Project) {
            if (registered) {
                return
            }
            // TODO add a editor null check here, there are cases here npe
            val editor: Editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
            val caretModel: CaretModel = editor.caretModel
            caretModel.addCaretListener(EditorCaretListener(project))
            registered = true
        }
    }

    private val infoView = project.service<ExternalInfoViewService>()

    override fun caretPositionChanged(event: CaretEvent) {
        val editor = event.editor
        val file = editor.virtualFile
        // TODO handle null
        val project = event.editor.project!!
        if (!file.path.endsWith(".lean")) {
            return
        }
        try {
            val plainGoal = LeanLspServerManager.getInstance(project).getPlainGoal(file, event.caret!!)
            val interactiveGoal = LeanLspServerManager.getInstance(project).getInteractiveGoals(file, event.caret!!)
            LeanInfoViewWindowFactory.updateGoal(project, plainGoal)
        } catch (e: Exception) {
            // TODO handle it
            e.printStackTrace()
        }

        try {
            // TODO DIY
            val logicalPosition = event.caret!!.logicalPosition
            val position = Position(line=logicalPosition.line, character = logicalPosition.column)
            infoView.changedCursorLocation(CursorLocation(
                uri = LeanLspServerManager.tryFixWinUrl(file.toString()),
                range = Range(start = position, end=position)
            ))
        } catch (e: Exception) {
            // TODO handle it
            e.printStackTrace()
        }



    }
}