package lean4ij.actions

import com.intellij.find.EditorSearchSession
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lean4ij.infoview.LeanInfoviewService
import lean4ij.util.leanProjectScope

class FindInInternalInfoview: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val infoviewService = project.service<LeanInfoviewService>()
        project.leanProjectScope.launch(Dispatchers.EDT) {
            // from https://stackoverflow.com/questions/53065010/how-to-create-an-intellij-plugin-which-can-simulate-the-ctrl-f-find-function
            val editor = infoviewService.toolWindow?.getEditor()?:return@launch
            val existedSearch = EditorSearchSession.get(editor)
            if (existedSearch != null) {
                // if it's already opened, close it
                // mainly this is for the button in toolbar
                existedSearch.close()
                return@launch
            }
            val searchSession = EditorSearchSession.start(editor, project)
            val searchTextComponent = searchSession.component.searchTextComponent
            searchTextComponent.requestFocus()
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT;
    }

    init {
        templatePresentation.icon = AllIcons.Actions.Find
    }
}