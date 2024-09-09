package lean4ij.infoview

import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import kotlinx.coroutines.CoroutineScope
import lean4ij.lsp.data.*
import java.awt.Color

/**
 * TODO this class should require some refactor
 */
class InfoviewMouseMotionListener(
    private val scope: CoroutineScope,
    private val infoViewWindow: LeanInfoViewWindow,
    private val support: EditorHyperlinkSupport,
    private val file: VirtualFile,
    private val logicalPosition: LogicalPosition,
    private val interactiveGoals: InteractiveGoals?,
    private val interactiveTermGoal: InteractiveTermGoal?,
    private val interactiveDiagnostics: List<InteractiveDiagnostics>?
) : EditorMouseMotionListener {
    private var hyperLink: RangeHighlighter? = null
    override fun mouseMoved(e: EditorMouseEvent) {
        if (hyperLink != null) {
            support.removeHyperlink(hyperLink!!)
        }
        if (!e.isOverText) {
            return
        }
        var c : CodeWithInfos? = null
        if (interactiveGoals != null) {
            c = interactiveGoals.getCodeText(e.offset)
        }
        if (c == null && interactiveTermGoal != null) {
            c = interactiveTermGoal.getCodeText(e.offset)
        }
//        // TODO make c and d both TaggedText
//        var d : TaggedText<MsgEmbed>? = null
//        if (c == null && interactiveDiagnostics != null) {
//            for (diagnostic in interactiveDiagnostics) {
//                d = diagnostic.message.getCodeText(e.offset)
//                if (d != null) {
//                    break
//                }
//            }
//        }
//        if (c == null && d == null) {
        if (c == null) {
            return
        }
        var codeWithInfosTag : CodeWithInfosTag? = null
        // TODO check if these parent-stuff can be cleaner
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

        // TODO check if these parent-stuff can be cleaner
        if (c.parent == null) {
            return
        }
        hyperLink = support.createHyperlink(
            c.parent!!.startOffset,
            c.parent!!.endOffset,
            object : TextAttributes() {
                override fun getBackgroundColor(): Color {
                    // TODO document this
                    // TODO should scheme be cache?
                    val scheme = EditorColorsManager.getInstance().globalScheme
                    // TODO customize attr? or would backgroundColor null?
                    //      indeed here it can be null, don't know why Kotlin does not mark it as error
                    return scheme.getAttributes(EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES).backgroundColor
                }
            },
            CodeWithInfosDocumentationHyperLink(scope, infoViewWindow, file, logicalPosition, codeWithInfosTag,
                RelativePoint(e.mouseEvent) )
        )
    }
}