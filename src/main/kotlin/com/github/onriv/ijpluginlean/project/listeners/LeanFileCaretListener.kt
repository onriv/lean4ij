package com.github.onriv.ijpluginlean.project.listeners

import com.github.onriv.ijpluginlean.project.LeanProjectService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class LeanFileCaretListener(private val project: Project) : CaretListener {

    private val leanProjectService : LeanProjectService = project.service()

    /**
     * TODO maybe debouncing here
     */
    override fun caretPositionChanged(event: CaretEvent) {
        leanProjectService.file(event.editor.virtualFile).updateCaret(event.newPosition)
    }

    private var editor: Editor? = null

    fun update(editor: Editor) {
        editor.caretModel.addCaretListener(this)
        this.editor?.caretModel?.removeCaretListener(this)
        this.editor = editor
    }
}