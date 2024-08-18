package lean4ij.infoview

import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import lean4ij.lsp.data.CodeWithInfosTag
import lean4ij.lsp.data.InteractiveGoals
import java.awt.Color

/**
 * TODO this class should require some refactor
 */
class InfoviewMouseMotionListener(
    private val infoViewWindow: LeanInfoViewWindow,
    private val support: EditorHyperlinkSupport,
    private val file: VirtualFile,
    private val logicalPosition: LogicalPosition,
    private val interactiveGoals: InteractiveGoals
) : EditorMouseMotionListener {
    private var hyperLink: RangeHighlighter? = null
    override fun mouseMoved(e: EditorMouseEvent) {
        if (hyperLink != null) {
            support.removeHyperlink(hyperLink!!)
        }
        if (!e.isOverText) {
            return
        }
        val c = interactiveGoals.getCodeText(e.offset) ?: return
        var codeWithInfosTag : CodeWithInfosTag? = null
        if (c is CodeWithInfosTag) {
            codeWithInfosTag = c
        } else if (c.parent != null && c.parent!! is CodeWithInfosTag) {
            codeWithInfosTag = c.parent!! as CodeWithInfosTag
        } else if (c.parent != null && c.parent!!.parent != null && c.parent!!.parent!! is CodeWithInfosTag) {
            codeWithInfosTag = c.parent!!.parent!! as CodeWithInfosTag
        }
        if (codeWithInfosTag == null) {
            return
        }

        if (c.parent == null || c.parent!!.parent == null) {
            return
        }
        hyperLink = support.createHyperlink(
            c.parent!!.startOffset,
            c.parent!!.endOffset,
            object : TextAttributes() {
                override fun getBackgroundColor(): Color {
                    return Color.decode("#add6ff")
                }
            },
            CodeWithInfosDocumentationHyperLink(infoViewWindow, file, logicalPosition, codeWithInfosTag,
                RelativePoint(e.mouseEvent) )
        )
    }
}