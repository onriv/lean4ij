package com.github.onriv.ijpluginlean.listeners

import com.github.onriv.ijpluginlean.lsp.LeanLspServerManager
import com.github.onriv.ijpluginlean.toolWindow.LeanInfoViewWindowFactory
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

class EditorCaretListener : CaretListener {

    companion object {

        private var registered = false

        @Synchronized
        fun register(project: Project) {
            if (registered) {
                return
            }
            // TODO add a editor null check here, there are cases here npe
            val editor: Editor = FileEditorManager.getInstance(project).selectedTextEditor!!
            val caretModel: CaretModel = editor.caretModel
            caretModel.addCaretListener(EditorCaretListener())
            registered = true
        }
    }

    override fun caretPositionChanged(event: CaretEvent) {
        val editor = event.editor
        val file = editor.virtualFile
        // TODO handle null
        val project = event.editor.project!!
        if (!file.path.endsWith(".lean")) {
            return
        }
        val plainGoal = LeanLspServerManager.getInstance(project).getPlainGoal(file, event.caret!!)
        val interactiveGoal = LeanLspServerManager.getInstance(project).getInteractiveGoals(file, event.caret!!)
        LeanInfoViewWindowFactory.updateGoal(project, plainGoal)
    }
}