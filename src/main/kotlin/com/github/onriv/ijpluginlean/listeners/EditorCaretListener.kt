package com.github.onriv.ijpluginlean.listeners

import com.github.onriv.ijpluginlean.lsp.LeanLspServerManager
import com.github.onriv.ijpluginlean.toolWindow.LeanInfoViewWindowFactory
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBTextArea
import java.util.concurrent.atomic.AtomicBoolean

class EditorCaretListener : CaretListener {

    companion object {

        private var registered = false

        @Synchronized
        fun register(project: Project) {
            if (registered) {
                return
            }
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

        // from https://stackoverflow.com/questions/66548934/how-to-access-components-inside-a-custom-toolwindow-from-an-actios
        val infoViewWindow = ToolWindowManager.getInstance(project).getToolWindow("LeanInfoViewWindow")!!.contentManager.contents[0].component as
                LeanInfoViewWindowFactory.LeanInfoViewWindow
        infoViewWindow.updateGoal(plainGoal.joinToString("\n"))
    }
}