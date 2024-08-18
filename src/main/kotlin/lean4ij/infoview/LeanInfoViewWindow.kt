package lean4ij.infoview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import javax.swing.BorderFactory
import javax.swing.JEditorPane

/**
 * check :https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html#ui-dsl-basics
 * for some cleaner way to write ui stuff
 */
class LeanInfoViewWindow(val toolWindow: ToolWindow) : SimpleToolWindowPanel(true) {

    private val goals = JEditorPane()
    val editor : EditorEx = createEditor()

    private val BORDER = BorderFactory.createEmptyBorder(3, 0, 5, 0)

    // TODO
    private fun render(map: Map<*, *>): String {
        for (g in map["goals"] as List<*>) {
            return ""
        }
        return ""
    }

    /**
     * check the following links about thread and ui
     * - https://intellij-support.jetbrains.com/hc/en-us/community/posts/360009458040-Error-writing-data-in-a-tree-provided-by-a-background-thread
     * - https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html
     * Once it's not update but now the method revalidate and updateUI seem not required now.
     */
    fun updateGoal(goal: String) {
        // for thread model and update ui:
        ApplicationManager.getApplication().invokeLater {
            editor.document.setText(goal)
            goals.text = goal
        }
    }

    /**
     * create an editorEx for rendering the info view
     */
    private fun createEditor(): EditorEx {
        val editor = EditorFactory.getInstance()
            .createViewer(DocumentImpl(" ", true), toolWindow.project) as EditorEx
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
        editor.setVerticalScrollbarVisible(true)
        editor.isRendererMode = true
        return editor
    }

}