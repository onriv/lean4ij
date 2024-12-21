package lean4ij.infoview

import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener

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
