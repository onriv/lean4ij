package lean4ij.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import lean4ij.infoview.LeanInfoViewWindowFactory
import lean4ij.util.LeanUtil

class ToggleLeanInfoViewInternal : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val leanInfoview = LeanInfoViewWindowFactory.getLeanInfoview(project)?:return
        val toolWindow = leanInfoview.toolWindow
        if (toolWindow.isVisible) {
            toolWindow.hide()
        } else {
            toolWindow.show()
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.dataContext.getData<Editor>(CommonDataKeys.EDITOR)?:return
        if (!LeanUtil.isLeanFile(editor.virtualFile)) {
            e.presentation.isVisible = false
        }
    }
}