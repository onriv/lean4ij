package com.github.onriv.ijpluginlean.toolWindow

import com.github.onriv.ijpluginlean.services.ExternalInfoViewService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.github.onriv.ijpluginlean.services.MyProjectService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBTextArea
import javax.swing.BorderFactory
import javax.swing.JEditorPane

// TODO not shown if indexing, the doc seems saying it's an option for it
class LeanInfoViewWindowFactory : ToolWindowFactory {

    companion object {
        fun updateGoal(project: Project, plainGoal: List<String>, plainTermGoal: String) {
            // from https://stackoverflow.com/questions/66548934/how-to-access-components-inside-a-custom-toolwindow-from-an-actios
            val infoViewWindow = ToolWindowManager.getInstance(project).getToolWindow("LeanInfoViewWindow")!!.contentManager.contents[0].component as
                    LeanInfoViewWindowFactory.LeanInfoViewWindow
            infoViewWindow.updateGoal(plainGoal.joinToString("\n\n")+"\n\n"+plainTermGoal)
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

    class LeanInfoViewWindow(toolWindow: ToolWindow) : SimpleToolWindowPanel(true) {

        private val service = toolWindow.project.service<MyProjectService>()
        private val infoViewService = toolWindow.project.service<ExternalInfoViewService>()
        private val goals = JEditorPane()
        private var editor : EditorEx

        private val BORDER = BorderFactory.createEmptyBorder(3, 0, 5, 0)

        init {
            // TODO this is copy from intellij-arend and it's wrong (it's fro SearchRender in intellij-arend)
            goals.contentType = "text/html"
            goals.border = BORDER
            goals.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            goals.font = JBTextArea().font

            editor = EditorFactory.getInstance().createViewer(DocumentImpl(" ", true), toolWindow.project) as EditorEx
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

            setContent(editor.component)
            // TODO this is commented out, it's no easy to impl
            //      interactive goal inside intellij idea
            // editor.addEditorMouseMotionListener(InfoViewHoverListener())
            // TODO it must setup a language for the goal infoview...
            // editor = EditorFactory.
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
    }
}
