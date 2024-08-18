package lean4ij.infoview

import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import lean4ij.lsp.data.InteractiveGoals
import lean4ij.lsp.data.InteractiveTermGoal


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
        fun updateInteractiveGoal(project: Project, file: VirtualFile?, logicalPosition: LogicalPosition, // TODO this should add some UT for the rendering
                                  interactiveGoals: InteractiveGoals?, interactiveTermGoal : InteractiveTermGoal?) {
            if (file == null) {
                return
            }
            val infoViewWindow = getLeanInfoview(project) ?: return
            // TODO implement the fold/open logic
            val interactiveInfoBuilder = StringBuilder("▼ ${file.name}:${logicalPosition.line+1}:${logicalPosition.column}\n")
            // TODO here maybe null?
            if (interactiveGoals != null || interactiveTermGoal != null) {
                interactiveGoals?.toInfoViewString(interactiveInfoBuilder)
                interactiveTermGoal?.toInfoViewString(interactiveInfoBuilder)
            } else {
                interactiveInfoBuilder.append("No info found.\n")
            }

            // TODO render message
            // TODO this seems kind of should be put inside rendering, check how to do this
            ApplicationManager.getApplication().invokeLater {
                val infoViewWindowEditorEx: EditorEx = infoViewWindow.createEditor()
                infoViewWindowEditorEx.document.setText(interactiveInfoBuilder.toString())
                val support = EditorHyperlinkSupport.get(infoViewWindowEditorEx)
                infoViewWindow.setContent(infoViewWindowEditorEx.component)
                // TODO does it require new object for each update?
                //      it seems so, otherwise the hyperlinks seems mixed and requires remove
                val mouseMotionListener = InfoviewMouseMotionListener(infoViewWindow, support, file, logicalPosition, interactiveGoals, interactiveTermGoal)
                infoViewWindowEditorEx.addEditorMouseMotionListener(mouseMotionListener)
            }
        }

    }

    init {
        thisLogger().info("create infoview window using swing")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val leanInfoViewWindow = LeanInfoViewWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(leanInfoViewWindow, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true


}
