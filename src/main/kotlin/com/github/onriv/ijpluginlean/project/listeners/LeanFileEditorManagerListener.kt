package com.github.onriv.ijpluginlean.project.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class LeanFileEditorManagerListener(private val project: Project) : FileEditorManagerListener {

    private val leanFileCaretListener : LeanFileCaretListener = project.service()

    override fun selectionChanged(event: FileEditorManagerEvent) {
        event.manager.selectedTextEditor?.let {
            leanFileCaretListener.update(it)
        }
    }
}