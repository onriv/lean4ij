package lean4ij.infoview

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import lean4ij.lsp.data.ContextInfo
import lean4ij.lsp.data.Position

/**
 * Service to manage the state and lifecycle of the Lean InfoView.
 *
 * This service acts as a central repository for the InfoView UI components and state,
 * facilitating communication between the tool window, actions, and the LSP client.
 *
 * TODO: Consider moving more logic from [InfoViewWindowFactory] here to better separate concerns (Model/Controller vs View).
 */
@Service(Service.Level.PROJECT)
class LeanInfoviewService(private val project: Project) {
   /**
    * Reference to the actual Swing window component of the InfoView.
    */
   var toolWindow: LeanInfoViewWindow? = null

   /**
    * Reference to the action toolbar component.
    */
   var actionToolbar: ActionToolbar? = null

   /**
    * Flag determining if the InfoView should automatically refresh on caret moves.
    */
   var automaticallyRefreshInternalInfoview = true

   /**
    * Tracks if the caret is currently over text in the InfoView (used for interaction logic).
    */
   var caretIsOverText: Boolean? = null

   /**
    * Holds current context information for the InfoView, including the file, position, and semantic info.
    * This is updated as the user interacts with the code or the InfoView.
    *
    * This is almost same as [InfoviewMouseMotionListener.offsetsFlow],
    * just with different types
    */
   var contextInfo : Triple<ContextInfo, VirtualFile, Position>? = null
}