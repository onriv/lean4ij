package com.github.onriv.ijpluginlean.listeners

import com.github.onriv.ijpluginlean.lsp.LeanLspServerManager
import com.github.onriv.ijpluginlean.lsp.data.Position
import com.github.onriv.ijpluginlean.services.CursorLocation
import com.github.onriv.ijpluginlean.services.ExternalInfoViewService
import com.github.onriv.ijpluginlean.services.Range
import com.github.onriv.ijpluginlean.toolWindow.LeanInfoViewWindowFactory
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class EditorCaretListener(val project: Project) : CaretListener {

    companion object {

        private val listeners: ConcurrentHashMap<Editor, EditorCaretListener> = ConcurrentHashMap();

        @Synchronized
        fun register(project: Project) {
            // TODO what's the different with Editor and FileEditor?
            // TODO real log
            val editor: Editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
            listeners.computeIfAbsent(editor) {
                val caretModel = editor.caretModel
                val listener = EditorCaretListener(project)
                caretModel.addCaretListener(listener)
                listener
            }
        }
    }

    private val infoView = project.service<ExternalInfoViewService>()


    private fun shouldUpdateGoal(file: VirtualFile, caret: Caret) : Boolean {
        if (!file.path.endsWith(".lean")) {
            return false
        }

        // TODO getting text seems to be a performance issue?
        // TODO is it instant data or from file or from memory?
        // TODO not fix encoding, handle bom?
        val lineContent = String(file.contentsToByteArray(), StandardCharsets.UTF_8).split(System.lineSeparator())[caret.logicalPosition.line]
//        val document = FileDocumentManager.getInstance().getDocument(file)
//        if (document == null) {
//            thisLogger().debug("${file.path} has no document in FileDocumentManager.")
//            return false
//        }
//        // TODO getting text seems to be a performance issue?
//        val lineContent = document.text.split("\n")[caret.logicalPosition.line]
        if (lineContent.startsWith("import ")) {
            thisLogger().debug("${file.path} with caret at import line, skip updating goal")
            return false
        }
        return true
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun caretPositionChanged(event: CaretEvent) {
        val editor = event.editor
        val file = editor.virtualFile
        // TODO not really understand why here it can be null
        if (event.caret == null) {
            thisLogger().debug("${file.path} moved with caret null.")
//                return@launch
        }
        thisLogger().debug("${file.path} moved to ${event.caret}")
        if (event.editor.project == null) {
            thisLogger().debug("${file.path} moved with project null.")
        }
        // TODO handle null
        val project = event.editor.project!!
//                return@launch
        // or, withContext(Dispatchers.IO) ?
        // TODO check kotlin's coroutine document
        //      it's no way start coroutine from normal thread except runBlocking
        //      see: https://stackoverflow.com/questions/76187252/
        runBackgroundableTask("Updating goal", project) {
            val caret = event.caret!!
            if (!shouldUpdateGoal(file, caret)) {
                return@runBackgroundableTask
            }
            try {
                val plainGoal = LeanLspServerManager.getInstance(project).plainGoal(file, caret)
                val plainTermGoal = LeanLspServerManager.getInstance(project).plainTermGoal(file, caret)
                LeanInfoViewWindowFactory.updateGoal(project, plainGoal, plainTermGoal)
                //  Error response from server: org.eclipse.lsp4j.jsonrpc.ResponseErrorException: Outdated RPC sessios
                // val interactiveGoal = LeanLspServerManager.getInstance(project).getInteractiveGoals(file, event.caret!!)
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
}