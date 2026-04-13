package lean4ij.infoview

import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener

/**
 * Handles mouse click events in the internal infoview editor.
 *
 * When the user clicks on a term in the infoview, this listener finds the deepest
 * [InfoObjectModel] node at the click offset and invokes its click action. Click actions
 * are used for interactive features like:
 * - Expanding lazy trace children (clicking on a collapsed trace node fetches and inserts
 *   its children)
 * - Other interactive widget behaviors defined by the Lean server
 *
 * @param context The infoview context containing the rendered content tree
 */
class InfoviewMouseListener(private val context: LeanInfoviewContext) : EditorMouseListener {

    override fun mousePressed(event: EditorMouseEvent) {
        super.mousePressed(event)
    }

    override fun mouseClicked(event: EditorMouseEvent) {
        context.rootObjectModel.getChild(event.offset)?.click(context)
    }

    override fun mouseReleased(event: EditorMouseEvent) {
        super.mouseReleased(event)
    }

    override fun mouseEntered(event: EditorMouseEvent) {
        super.mouseEntered(event)
    }

    override fun mouseExited(event: EditorMouseEvent) {
        super.mouseExited(event)
    }
}
