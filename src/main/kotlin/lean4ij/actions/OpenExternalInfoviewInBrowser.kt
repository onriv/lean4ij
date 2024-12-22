package lean4ij.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.util.IconLoader
import lean4ij.infoview.external.JcefInfoviewService
import lean4ij.util.notify

class OpenExternalInfoviewInBrowser : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    init {
        templatePresentation.icon = IconLoader.getIcon("/icons/inlayGlobe.svg", javaClass)
    }

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