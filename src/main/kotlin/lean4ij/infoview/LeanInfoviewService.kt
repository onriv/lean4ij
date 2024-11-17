package lean4ij.infoview

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * TODO move some logic in [LeanInfoViewWindowFactory] here
 */
@Service(Service.Level.PROJECT)
class LeanInfoviewService(private val project: Project) {
   var toolWindow: LeanInfoViewWindow? = null
   var actionToolbar: ActionToolbar? = null
   var automaticallyRefreshInternalInfoview = true
}