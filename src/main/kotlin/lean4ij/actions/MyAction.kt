package lean4ij.actions

// import lean4ij.listeners.EditorCaretListener
// import lean4ij.lsp.LeanLanguageServer
// import lean4ij.lsp.LeanLspServerManager
// import lean4ij.lsp.LeanLspServerSupportProvider
// import lean4ij.lsp.RpcConnectParams
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

