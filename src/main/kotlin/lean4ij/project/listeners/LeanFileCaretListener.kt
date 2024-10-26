package lean4ij.project.listeners

import lean4ij.project.LeanProjectService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import lean4ij.util.LeanUtil
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

@Service(Service.Level.PROJECT)
class LeanFileCaretListener(private val project: Project) : CaretListener/*, PropertyChangeListener */{

    private val leanProjectService : LeanProjectService = project.service()

    /**
     * TODO maybe debouncing here
     */
    override fun caretPositionChanged(event: CaretEvent) {
        // TODO passing things like editor etc seems cumbersome, maybe add some implement for context
        if (!LeanUtil.isLeanFile(event.editor.virtualFile.path)) {
            return
        }
        leanProjectService.file(event.editor.virtualFile).updateCaret(event.editor, event.newPosition)
    }

    private var editor: Editor? = null

    fun update(editor: Editor) {
        editor.caretModel.addCaretListener(this)
        this.editor?.caretModel?.removeCaretListener(this)
        this.editor = editor
    }
}