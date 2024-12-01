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
            leanInfoViewWindow.updateEditorMouseMotionListenerV1(this@LeanInfoviewContext)
        }
    }
}