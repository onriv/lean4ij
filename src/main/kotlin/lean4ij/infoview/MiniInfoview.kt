package lean4ij.infoview

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lean4ij.project.LeanProjectService
import java.awt.Dimension

/**
 * A lightweight, floating infoview panel that shows the current goal type near the cursor.
 *
 * Unlike the full [LeanInfoViewWindow] which lives in a tool window, the mini-infoview
 * appears as a small popup near the caret position, showing just the goal's "⊢ type" line.
 * This provides quick feedback without switching focus to the infoview panel.
 *
 * The editor is created on EDT and wrapped in a [CompletableDeferred] since the panel may
 * be accessed before EDT initialization completes. The editor has scrollbars, caret, and
 * caret-row highlighting disabled for a minimal appearance.
 *
 * @see MiniInfoviewService for the lifecycle and popup management
 */
class MiniInfoview(val project: Project) : SimpleToolWindowPanel(true) {
    private val editor : CompletableDeferred<EditorEx> = CompletableDeferred()

    suspend fun getEditor(): EditorEx {
        return editor.await()
    }

    /**
     * This si for displaying popup expr
     */
    val leanProject = project.service<LeanProjectService>()
    init {
        leanProject.scope.launch(Dispatchers.EDT) {
            try {
                val editor0 = InfoViewEditorFactory(project).createEditor(showScroll = false)
                editor0.setCaretEnabled(false)
                editor0.setCaretVisible(false)
                editor0.colorsScheme.setColor(EditorColors.CARET_ROW_COLOR, null)
                editor.complete(editor0)
                setContent(editor0.component)
            } catch (ex: Throwable) {
                editor.completeExceptionally(ex)
            }
        }
    }

    suspend fun measureIntrinsicContentSize(): Dimension {
        val editor = getEditor()

        return withContext(Dispatchers.EDT) {
            val document = editor.document
            val text = document.text
            val lines = text.split("\n")

            val fontMetrics = editor.contentComponent.getFontMetrics(editor.colorsScheme.getFont(EditorFontType.PLAIN))
            val maxWidth = lines.maxOfOrNull { fontMetrics.stringWidth(it) } ?: 0
            val lineHeight = editor.lineHeight
            val totalHeight = lineHeight * lines.size

            Dimension(maxWidth + 40, totalHeight + 5)
        }
    }
}