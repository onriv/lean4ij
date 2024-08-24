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
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

@Service(Service.Level.PROJECT)
class LeanFileCaretListener(private val project: Project) : CaretListener/*, PropertyChangeListener */{

    private val leanProjectService : LeanProjectService = project.service()

    /**
     * TODO maybe debouncing here
     */
    override fun caretPositionChanged(event: CaretEvent) {
        leanProjectService.file(event.editor.virtualFile).updateCaret(event.newPosition)
    }

    private var editor: Editor? = null

    fun update(editor: Editor) {
        // TODO is it better way to get editor font change event?
        //      ref: https://intellij-support.jetbrains.com/hc/en-us/community/posts/16305446476562-How-can-I-be-notified-when-a-user-has-changed-the-font-size
        // (editor as EditorEx).addPropertyChangeListener(this)
        // this.editor?.let { (it as EditorEx).removePropertyChangeListener(this) }

        editor.caretModel.addCaretListener(this)
        this.editor?.caretModel?.removeCaretListener(this)
        this.editor = editor
    }
    //
    // override fun propertyChange(evt: PropertyChangeEvent?) {
    //     println(evt)
    // }
}