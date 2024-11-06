package lean4ij.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import lean4ij.infoview.LeanInfoViewWindowFactory
import lean4ij.infoview.external.JcefInfoviewService
import lean4ij.infoview.external.JcefInfoviewTooWindowFactory
import lean4ij.util.LeanUtil
import lean4ij.util.notify

class ToggleLeanInfoViewInternal : AnAction() {

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

class ToggleLeanInfoViewJcef : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = JcefInfoviewTooWindowFactory.getToolWindow(project)?:return
        if (toolWindow.isVisible) {
            toolWindow.hide()
        } else {
            toolWindow.show()
        }
    }
}

class OpenExternalInfoviewInBrowser : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.service<JcefInfoviewService>()
        if (service.url == null) {
            project.notify("url for the external infoview is null. Please check build toolwindow for if the external infoview service starts or not.")
            return
        }
        BrowserUtil.browse(service.url!!)
    }
}

class IncreaseZoomLevelForLeanInfoViewJcef : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.service<JcefInfoviewService>()
        service.increaseZoomLevel()
    }
}

class DecreaseZoomLevelForLeanInfoViewJcef : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.service<JcefInfoviewService>()
        service.decreaseZoomLevel()
    }
}
class ResetZoomLevelForLeanInfoViewJcef : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.service<JcefInfoviewService>()
        service.resetZoomLevel()
    }
}
