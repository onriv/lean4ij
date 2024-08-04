package com.github.onriv.ijpluginlean.actions

// import com.github.onriv.ijpluginlean.listeners.EditorCaretListener
// import com.github.onriv.ijpluginlean.lsp.LeanLanguageServer
// import com.github.onriv.ijpluginlean.lsp.LeanLspServerManager
// import com.github.onriv.ijpluginlean.lsp.LeanLspServerSupportProvider
// import com.github.onriv.ijpluginlean.lsp.RpcConnectParams
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages

class MyCustomAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        // Display a dialog with 'Hello World!'
        Messages.showMessageDialog("Hello World!", "Greeting", Messages.getInformationIcon())
    }

    override fun update(e: AnActionEvent) {
        val editor = e.dataContext.getData("editor") as? Editor
        if (e.project == null || editor == null) {
            return
        }
        val caret: Caret = editor.caretModel.primaryCaret
        val offset = caret.offset
        // println("Cursor moved to offset: $offset")
    }
}

