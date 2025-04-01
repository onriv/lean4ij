package lean4ij.infoview

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lean4ij.project.LeanProjectService
import javax.swing.BorderFactory

class MiniInfoview(val project: Project) : SimpleToolWindowPanel(true) {
    /**
     * TODO make this private
     */
    private val editor : CompletableDeferred<EditorEx> = CompletableDeferred()

    suspend fun getEditor(): EditorEx {
        return editor.await()
    }

    /**
     * This si for displaying popup expr
     */
    val leanProject = project.service<LeanProjectService>()
    init {
        leanProject.scope.launch(Dispatchers.EDT) {
            try {
                val editor0 = InfoViewEditorFactory(project).createEditor()
                editor.complete(editor0)
                setContent(editor0.component)
            } catch (ex: Throwable) {
                editor.completeExceptionally(ex)
            }
        }
    }
}
