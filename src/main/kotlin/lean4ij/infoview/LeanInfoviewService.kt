package lean4ij.infoview

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import lean4ij.lsp.data.ContextInfo
import lean4ij.lsp.data.Position

/**
 * TODO move some logic in [InfoViewWindowFactory] here
 */
@Service(Service.Level.PROJECT)
class LeanInfoviewService(private val project: Project) {
   var toolWindow: LeanInfoViewWindow? = null
   var actionToolbar: ActionToolbar? = null
   var automaticallyRefreshInternalInfoview = true

   var caretIsOverText: Boolean? = null

   /**
    * This is almost same as [InfoviewMouseMotionListener.offsetsFlow],
    * just with different types
    */
   var contextInfo : Triple<ContextInfo, VirtualFile, Position>? = null
}