package lean4ij.infoview

import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lean4ij.infoview.dsl.InfoObjectModel
import lean4ij.lsp.data.Position
import lean4ij.project.LeanProjectService

/**
 * Immutable snapshot of all data needed to render and interact with a single infoview state.
 *
 * Each time the infoview content updates (e.g., caret moves to a new position), a new
 * [LeanInfoviewContext] is created and passed to the rendering pipeline. This ensures that
 * mouse listeners and popup handlers always reference consistent state, even if the user
 * moves the caret again before interaction completes.
 *
 * ## Fields
 * - [leanProject]: Project service for accessing coroutine scope and language server
 * - [leanInfoViewWindow]: The tool window panel where content is rendered
 * - [infoviewEditor]: The specific [EditorEx] instance displaying this content
 * - [file]: The Lean source file this infoview state corresponds to
 * - [position]: The cursor position in the source file
 * - [rootObjectModel]: The rendered content tree (goals, diagnostics, etc.)
 *
 * ## Refresh
 * Calling [refresh] re-renders the current [rootObjectModel] into the editor on the EDT,
 * which is used after lazy trace expansion to update the display without re-fetching data.
 *
 * TODO some fields are in fact unnecessary
 */
class LeanInfoviewContext(
    val leanProject: LeanProjectService,
    val leanInfoViewWindow: LeanInfoViewWindow,
    val infoviewEditor: EditorEx,
    val file: VirtualFile,
    val position: Position,
    val rootObjectModel: InfoObjectModel
) {
    fun refresh() {
        leanProject.scope.launch(Dispatchers.EDT) {
            leanInfoViewWindow.updateEditorMouseMotionListener(this@LeanInfoviewContext)
        }
    }
}