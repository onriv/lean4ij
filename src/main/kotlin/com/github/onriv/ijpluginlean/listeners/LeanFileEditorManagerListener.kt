package com.github.onriv.ijpluginlean.listeners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class LeanFileEditorManagerListener : FileEditorManagerListener {

    override fun selectionChanged(event: FileEditorManagerEvent) {
        // event.newEditor?.let{EditorCaretListener.register(event.manager.project)}
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        // TODO log here
        // EditorCaretListener.register(source.project)
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        // TODO do remove logic
        super.fileClosed(source, file)
    }
}