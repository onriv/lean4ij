package lean4ij.toolWindow

// https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html#ui-dsl-basics
import lean4ij.lsp.data.InteractiveGoals
import com.google.gson.Gson
// import lean4ij.lsp.data.gson
// import lean4ij.services.ExternalInfoViewService
// // import lean4ij.services.ExternalInfoViewService
// import lean4ij.services.MyProjectService
// import com.intellij.codeInsight.documentation.DocumentationHtmlUtil.docPopupPreferredMaxWidth
// import com.intellij.codeInsight.documentation.DocumentationHtmlUtil.docPopupPreferredMinWidth
// import com.intellij.execution.filters.HyperlinkInfo
// import com.intellij.execution.filters.ShowTextPopupHyperlinkInfo
import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import javax.swing.*


// TODO not shown if indexing, the doc seems saying it's an option for it
class LeanInfoViewWindowFactory : ToolWindowFactory {

    companion object {
        fun updateGoal(project: Project, file: VirtualFile, caret: Caret, plainGoal: List<String>, plainTermGoal: String) {
            // from https://stackoverflow.com/questions/66548934/how-to-access-components-inside-a-custom-toolwindow-from-an-actios
            val infoViewWindow = ToolWindowManager.getInstance(project).getToolWindow("LeanInfoViewWindow")!!.contentManager.contents[0].component as
                    LeanInfoViewWindowFactory.LeanInfoViewWindow

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

        fun updateInteractiveGoal(project: Project, file: VirtualFile?, caret: Caret, interactiveGoalsAny: Any) {
            if (file == null) {
                return
            }
            // from https://stackoverflow.com/questions/66548934/how-to-access-components-inside-a-custom-toolwindow-from-an-actios
            // TODO it seems if the toolwindows not opened before, it contains no content
            val contents = ToolWindowManager.getInstance(project).getToolWindow("LeanInfoViewWindow")!!.contentManager.contents
            // TODO confirm this
            if (contents.isEmpty()) {
                return
            }
            val infoViewWindow = contents[0].component as
                    LeanInfoViewWindowFactory.LeanInfoViewWindow
            // TODO use GsonUtil
            val interactiveGoals : InteractiveGoals = Gson().fromJson(Gson().toJson(interactiveGoalsAny), InteractiveGoals::class.java)
            val interactiveGoalsBuilder = StringBuilder("▼ ${file.name}:${caret.logicalPosition.line+1}:${caret.logicalPosition.column}\n")
            val interactiveGoalsText = interactiveGoals.toInfoViewString(interactiveGoalsBuilder)

            ApplicationManager.getApplication().invokeLater {
                val infoViewWindowEditorEx: EditorEx = infoViewWindow.createEditor()
                infoViewWindowEditorEx.document.setText(interactiveGoalsText)
                val support = EditorHyperlinkSupport.get(infoViewWindowEditorEx)
                infoViewWindow.setContent(infoViewWindowEditorEx.component)
                // infoViewWindowEditorEx.addEditorMouseMotionListener(object : EditorMouseMotionListener {
                //     var hyperLink: RangeHighlighter? = null
                //     override fun mouseMoved(e: EditorMouseEvent) {
                //         if (hyperLink != null) {
                //             support.removeHyperlink(hyperLink!!)
                //         }
                //         if (!e.isOverText) {
                //             return
                //         }
                //         val c = interactiveGoals.getCodeText(e.offset) ?: return
                //         var codeWithInfosTag : CodeWithInfosTag? = null
                //         if (c is CodeWithInfosTag) {
                //             codeWithInfosTag = c
                //         } else if (c.parent != null && c.parent!! is CodeWithInfosTag) {
                //             codeWithInfosTag = c.parent!! as CodeWithInfosTag
                //         } else if (c.parent != null && c.parent!!.parent != null && c.parent!!.parent!! is CodeWithInfosTag) {
                //             codeWithInfosTag = c.parent!!.parent!! as CodeWithInfosTag
                //         }
                //         if (codeWithInfosTag == null) {
                //             return
                //         }
                //
                //         if (c.parent == null || c.parent!!.parent == null) {
                //             return
                //         }
                //         hyperLink = support.createHyperlink(
                //             c.parent!!.startOffset,
                //             c.parent!!.endOffset,
                //             object : TextAttributes() {
                //                 override fun getBackgroundColor(): Color {
                //                     return Color.decode("#add6ff")
                //                 }
                //             },
                //             CodeWithInfosDocumentationHyperLink(infoViewWindow, file!!, caret, codeWithInfosTag,
                //                 RelativePoint(e.mouseEvent) )
                //         )
                //     }
                // })
            }
        }

    }

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val leanInfoViewWindow = LeanInfoViewWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(leanInfoViewWindow, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class LeanInfoViewWindow(val toolWindow: ToolWindow) : SimpleToolWindowPanel(true) {


        private val goals = JEditorPane()
        private var editor : EditorEx = createEditor()

        private val BORDER = BorderFactory.createEmptyBorder(3, 0, 5, 0)

        // TODO
        private fun render(map: Map<*, *>): String {
            for (g in map["goals"] as List<*>) {
                return ""
            }
            return ""
        }

        fun updateGoal(goal: String) {
            // for thread model and update ui:
            // - https://intellij-support.jetbrains.com/hc/en-us/community/posts/360009458040-Error-writing-data-in-a-tree-provided-by-a-background-thread
            // - https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html
            ApplicationManager.getApplication().invokeLater {
                editor.document.setText(goal)
                goals.text = goal
            }
//            goals.revalidate();
//            goals.updateUI();
        }

        fun createEditor(): EditorEx {
            val editor = EditorFactory.getInstance().createViewer(DocumentImpl(" ", true), toolWindow.project) as EditorEx
            // val editor = editorTextField.getEditor(true)!!
            with(editor.settings) {
                isRightMarginShown = false
                isLineNumbersShown = false
                isLineMarkerAreaShown = false
                isRefrainFromScrolling = true
                isCaretRowShown = false
                isUseSoftWraps = true
                setGutterIconsShown(false)
                additionalLinesCount = 0
                additionalColumnsCount = 1
                isFoldingOutlineShown = false
                isVirtualSpace = false
            }
            editor.headerComponent = null
            editor.setCaretEnabled(false)
            editor.setHorizontalScrollbarVisible(false)
            editor.setVerticalScrollbarVisible(false)
            editor.isRendererMode = true
            return editor
        }

    }

}
