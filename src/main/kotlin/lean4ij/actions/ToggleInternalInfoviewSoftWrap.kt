package lean4ij.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.AbstractToggleUseSoftWrapsAction
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces
import com.intellij.openapi.util.IconLoader
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import lean4ij.infoview.LeanInfoviewService

/**
 * TODO all the actions should be nicely grouped
 *      ref: https://github.com/asciidoctor/asciidoctor-intellij-plugin/pull/222
 */
class ToggleInternalInfoviewSoftWrap : AbstractToggleUseSoftWrapsAction(SoftWrapAppliancePlaces.MAIN_EDITOR, false) {

    init {
        templatePresentation.icon = IconLoader.getIcon("/icons/newLine.svg", javaClass);

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