package lean4ij.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.AbstractToggleUseSoftWrapsAction
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces
import com.intellij.openapi.util.IconLoader.getIcon
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import lean4ij.infoview.LeanInfoViewWindowFactory
import lean4ij.infoview.LeanInfoviewService
import lean4ij.infoview.external.JcefInfoviewService
import lean4ij.infoview.external.JcefInfoviewTooWindowFactory
import lean4ij.setting.Lean4Settings
import lean4ij.util.LeanUtil
import lean4ij.util.notify

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

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT;
    }
}

class OpenExternalInfoviewInBrowser : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    init {
        templatePresentation.icon = getIcon("/icons/inlayGlobe.svg", javaClass)
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

class IncreaseZoomLevelForLeanInfoViewJcef : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    init {
        templatePresentation.icon = AllIcons.Graph.ZoomIn
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.service<JcefInfoviewService>()
        service.increaseZoomLevel()
    }
}

class DecreaseZoomLevelForLeanInfoViewJcef : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    init {
        templatePresentation.icon = AllIcons.Graph.ZoomOut
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.service<JcefInfoviewService>()
        service.decreaseZoomLevel()
    }
}

class ResetZoomLevelForLeanInfoViewJcef : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    init {
        templatePresentation.icon = AllIcons.Graph.ActualZoom
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.service<JcefInfoviewService>()
        service.resetZoomLevel()
    }
}

class ToggleLeanInfoviewJcefToolbarVisibility : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    init {
        templatePresentation.icon = getIcon("/icons/review_eye.svg", javaClass);
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.service<JcefInfoviewService>()
        val actionToolbar = service.actionToolbar?:return
        val component = actionToolbar.component
        component.isVisible = !component.isVisible
    }

}

class ToggleLeanInfoviewToolbarVisibility : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    init {
        templatePresentation.icon = getIcon("/icons/review_eye.svg", javaClass);
    }

    /**
     * TODO make a service for the Internal infoview
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val actionToolbar = project.service<LeanInfoviewService>().actionToolbar ?: return
        val component = actionToolbar.component
        component.isVisible = !component.isVisible
    }

}

/**
 * TODO all the actions should be nicely grouped
 *      ref: https://github.com/asciidoctor/asciidoctor-intellij-plugin/pull/222
 */
class ToggleInternalInfoviewSoftWrap : AbstractToggleUseSoftWrapsAction(SoftWrapAppliancePlaces.MAIN_EDITOR, false) {

    init {
        templatePresentation.icon = getIcon("/icons/newLine.svg", javaClass);

        // TODO not sure if it should call the method copy from, but
        //      it make the text into 'Soft-Wrap', which is wrong and cannot distinguish from the builtin action
        // copyFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_USE_SOFT_WRAPS));
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT;
    }

    override fun getEditor(e: AnActionEvent): Editor? {
        val project = e.project ?: return null
        val toolWindow = project.service<LeanInfoviewService>().toolWindow ?: return null
        return runBlocking {
            try {
                withTimeout(1000) {
                    toolWindow.getEditor()
                }
            } catch (ex: TimeoutCancellationException) {
                null
            }
        }
    }
}

class ToggleInfoviewPreferred : AnAction() {
    private val lean4Settings = service<Lean4Settings>()
    // TODO constant for the actions
    private val toggleLeanInfoViewInternal = ActionManager.getInstance().getAction("ToggleLeanInfoViewInternal")
    private val toggleLeanInfoViewJcef = ActionManager.getInstance().getAction("ToggleLeanInfoViewJcef")

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT;
    }

    override fun actionPerformed(e: AnActionEvent) {
        when {
            lean4Settings.preferredInfoview == "Jcef" ->
                    toggleLeanInfoViewJcef.actionPerformed(e)
            else ->
                    toggleLeanInfoViewInternal.actionPerformed(e)
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.dataContext.getData(CommonDataKeys.EDITOR)?:return
        // TODO here it can be null, weird
        val virtualFile = editor.virtualFile?: return
        if (!LeanUtil.isLeanFile(virtualFile)) {
            e.presentation.isVisible = false
        }
    }
}

class FindInExternalInfoview : AnAction() {

    init {
        templatePresentation.icon = AllIcons.Actions.Find
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val jcefInfoviewService = project.service<JcefInfoviewService>()
        jcefInfoviewService.searchTextField.isVisible = !jcefInfoviewService.searchTextField.isVisible
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT;
    }
}