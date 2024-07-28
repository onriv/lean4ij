package com.github.onriv.ijpluginlean.project.listeners

import com.github.onriv.ijpluginlean.project.file
import com.github.onriv.ijpluginlean.util.LeanUtil
import com.github.onriv.ijpluginlean.util.LspUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.project.Project
import kotlinx.coroutines.internal.AtomicOp
import java.util.concurrent.locks.ReentrantLock

@Service(Service.Level.PROJECT)
class LeanFileCaretListener(private val project: Project) : CaretListener {

    /**
     * TODO maybe debouncing here
     */
    override fun caretPositionChanged(event: CaretEvent) {
        project.file(event.editor.virtualFile).updateCaret(event.newPosition)
    }

    private var editor: Editor? = null

    fun update(editor: Editor) {
        editor.caretModel.addCaretListener(this)
        this.editor?.caretModel?.removeCaretListener(this)
        this.editor = editor
    }
}