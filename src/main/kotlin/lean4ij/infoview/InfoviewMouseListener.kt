package lean4ij.infoview

import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import lean4ij.lsp.data.InfoviewRender

class InfoviewMouseListener(private val infoviewRender: InfoviewRender) : EditorMouseListener {

    override fun mousePressed(event: EditorMouseEvent) {
        super.mousePressed(event)
    }

    override fun mouseClicked(event: EditorMouseEvent) {
        infoviewRender.getClickAction(event.offset)?.invoke(event)
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

class InfoviewMouseListenerV1(private val context: LeanInfoviewContext) : EditorMouseListener {

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
