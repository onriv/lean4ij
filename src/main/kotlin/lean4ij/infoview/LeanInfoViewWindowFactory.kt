package lean4ij.infoview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lean4ij.lsp.data.*
import lean4ij.project.LeanProjectService


/**
 * TODO do not show this if indexing, the doc seems saying it's an option for it
 *      maybe dumb-aware or something
 */
class LeanInfoViewWindowFactory : ToolWindowFactory {

    companion object {

        fun getLeanInfoview(project: Project): LeanInfoViewWindow? {
            val contents = ToolWindowManager.getInstance(project)
                .getToolWindow("LeanInfoViewWindow")!!
                .contentManager.contents
            if (contents.isEmpty()) {
                return null
            }
            return contents[0].component as LeanInfoViewWindow
        }

        /**
         * check https://stackoverflow.com/questions/66548934/how-to-access-components-inside-a-custom-toolwindow-from-an-actios
         * for how to access a custom tool window
         * TODO maybe impl displaying plainTermGoal too
         * TODO since using interactiveGoals for quite recent, this requires some test
         */
        fun updateGoal(project: Project, file: VirtualFile, caret: Caret, plainGoal: List<String>, plainTermGoal: String) {
            val infoViewWindow = getLeanInfoview(project) ?: return
            val contentBuilder = StringBuilder("▼ ${file.name}:${caret.logicalPosition.line+1}:${caret.logicalPosition.column}\n")
            if (plainGoal.isEmpty() && plainTermGoal.isEmpty()) {
                contentBuilder.append("No info found.\n")
            } else {
                if (plainGoal.isNotEmpty()) {
                    contentBuilder.append(" ▼ Tactic state\n")
                    if (plainGoal.size == 1) {
                        contentBuilder.append(" 1 goal\n")
                    } else {
                        contentBuilder.append(" ${plainGoal.size} goals\n")
                    }
                    for (s in plainGoal) {
                        contentBuilder.append(s)
                        contentBuilder.append("\n")
                    }
                }
                if (plainTermGoal.isNotEmpty()) {
                    contentBuilder.append(" ▼ Expected type\n")
                    contentBuilder.append(plainTermGoal)
                }
            }

            infoViewWindow.updateGoal(contentBuilder.toString())
        }

        /**
         * TODO the infoview-app
         */
        fun updateInteractiveGoal(
            project: Project,
            file: VirtualFile?,
            logicalPosition: LogicalPosition, // TODO this should add some UT for the rendering
            interactiveGoals: InteractiveGoals?,
            interactiveTermGoal: InteractiveTermGoal?,
            interactiveDiagnostics: List<InteractiveDiagnostics>?
        ) {
            if (file == null) {
                return
            }
            val infoViewWindow = getLeanInfoview(project) ?: return
            // TODO implement the fold/open logic
            val interactiveInfoBuilder = StringBuilder("▼ ${file.name}:${logicalPosition.line+1}:${logicalPosition.column}\n")
            // TODO here maybe null?
            if (interactiveGoals != null || interactiveTermGoal != null || !interactiveDiagnostics.isNullOrEmpty()) {
                interactiveGoals?.toInfoViewString(interactiveInfoBuilder)
                interactiveTermGoal?.toInfoViewString(interactiveInfoBuilder)
                if (!interactiveDiagnostics.isNullOrEmpty()) {
                    interactiveInfoBuilder.append("▼ Messages (${interactiveDiagnostics.size})\n")
                    interactiveDiagnostics.forEach { i ->
                        interactiveInfoBuilder.append("▼ ${file.name}:${i.fullRange.start.line+1}:${i.fullRange.start.character}\n")
                        i.toInfoViewString(interactiveInfoBuilder)
                        // TODO
                    }
                }
            } else {
                interactiveInfoBuilder.append("No info found.\n")
            }

            // TODO render message
            // TODO this seems kind of should be put inside rendering, check how to do this
            // TODO maybe it's too broad, maybe only createEditor in EDT
            val scope = project.service<LeanProjectService>().scope
            // The scope.launch here is mainly for the editor
            // ref: https://plugins.jetbrains.com/docs/intellij/coroutine-tips-and-tricks.html
            // TODO minimize the invoke later range
            scope.launch(Dispatchers.EDT) {
                infoViewWindow.updateEditorMouseMotionListener(interactiveInfoBuilder.toString(), file, logicalPosition, // TODO this should add some UT for the rendering
                    interactiveGoals, interactiveTermGoal)
            }
        }

    }

    init {
        thisLogger().info("create infoview window using swing")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        ApplicationManager.getApplication().invokeLater {
            // This should be run in EDT
            // TODO check if it's necessary to pull call in EDT
            //      or, is there better way to do it
            val leanInfoViewWindow = LeanInfoViewWindow(toolWindow)
            val content = ContentFactory.getInstance().createContent(leanInfoViewWindow, null, false)
            toolWindow.contentManager.addContent(content)
        }
    }

    override fun shouldBeAvailable(project: Project) = true


}
