package lean4ij.infoview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
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
         * TODO the implementation should absolutely be replaced by better rendering way
         *      using raw text it's very inconvenient to update things like hovering event
         *      but though vim/emacs has to do it this way maybe ...
         * TODO passing things like editor etc seems cumbersome, maybe add some implement for context
         */
        fun updateInteractiveGoal(
            editor: Editor,
            project: Project,
            file: VirtualFile?, // TODO this should add some UT for the rendering
            logicalPosition: LogicalPosition,
            interactiveGoals: InteractiveGoals?,
            interactiveTermGoal: InteractiveTermGoal?,
            interactiveDiagnostics: List<InteractiveDiagnostics>?,
            allMessage: List<InteractiveDiagnostics>?
        ) {
            if (file == null) {
                return
            }
            val infoViewWindow = getLeanInfoview(project) ?: return
            // TODO implement the fold/open logic
            val infoviewRender = InfoviewRender()
            val start = infoviewRender.length
            val header = "${file.name}:${logicalPosition.line+1}:${logicalPosition.column}"
            infoviewRender.append("${header}\n")
            // TODO here maybe null?
            // TODO refactor this
            if (interactiveGoals != null || interactiveTermGoal != null || !interactiveDiagnostics.isNullOrEmpty()) {
                interactiveGoals?.toInfoViewString(infoviewRender)
                interactiveTermGoal?.toInfoViewString(editor, infoviewRender)
                if (!interactiveDiagnostics.isNullOrEmpty()) {
                    val header = "Messages (${interactiveDiagnostics.size})"
                    val start = infoviewRender.length
                    infoviewRender.append("${header}\n")
                    interactiveDiagnostics.forEach { i ->
                        infoviewRender.append("▼ ${file.name}:${i.fullRange.start.line+1}:${i.fullRange.start.character}\n")
                        i.toInfoViewString(infoviewRender)
                        infoviewRender.append('\n')
                    }
                    // The last line break should not be folded, here the impl seems kind of adhoc
                    infoviewRender.deleteLastChar()
                    val end = infoviewRender.length
                    infoviewRender.addFoldingOperation(start, end, header)
                    infoviewRender.append('\n')
                }
            } else {
                infoviewRender.append("No info found.\n")
            }
            // The last line break should not be folded, here the impl seems kind of adhoc
            infoviewRender.deleteLastChar()
            val end = infoviewRender.length
            infoviewRender.addFoldingOperation(start, end, header)
            infoviewRender.append("\n")

            if (!allMessage.isNullOrEmpty()) {
                val header = "All Messages (${allMessage.size})"
                val start = infoviewRender.length
                infoviewRender.append("$header\n")
                allMessage.forEach { i ->
                    val header = "${file.name}:${i.fullRange.start.line}:${i.fullRange.start.character}"
                    val start = infoviewRender.length
                    infoviewRender.append("${header}\n")
                    i.toInfoViewString(infoviewRender)
                    val end = infoviewRender.length
                    infoviewRender.addFoldingOperation(start, end, header)
                    infoviewRender.append('\n')
                }
                infoviewRender.deleteLastChar()
                val end = infoviewRender.length
                infoviewRender.addFoldingOperation(start, end, header, false)
                infoviewRender.append("\n")
            }

            // TODO render message
            // TODO this seems kind of should be put inside rendering, check how to do this
            // TODO maybe it's too broad, maybe only createEditor in EDT
            val scope = project.service<LeanProjectService>().scope
            // The scope.launch here is mainly for the editor
            // ref: https://plugins.jetbrains.com/docs/intellij/coroutine-tips-and-tricks.html
            // TODO minimize the invoke later range
            scope.launch(Dispatchers.EDT) {
                infoViewWindow.updateEditorMouseMotionListener(infoviewRender, file, logicalPosition, // TODO this should add some UT for the rendering
                    interactiveGoals, interactiveTermGoal, interactiveDiagnostics, allMessage)
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
