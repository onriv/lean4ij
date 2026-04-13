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

/**
 * Listens for caret position changes in Lean files and triggers infoview updates.
 *
 * This is a project-level service that attaches itself as a [CaretListener] to the currently
 * active editor. When the caret moves in a Lean file, it delegates to [LeanFile.updateCaret]
 * which fetches interactive goals/diagnostics and updates all infoview panels.
 *
 * ## Editor Tracking
 * The [update] method is called by [LeanFileEditorManagerListener] whenever the active editor
 * changes. It moves the caret listener registration from the old editor to the new one,
 * ensuring only one editor is tracked at a time.
 *
 * @see LeanFile.updateCaret for the downstream update logic
 * @see LeanFileEditorManagerListener for the editor change trigger
 */
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
        if (this.editor == editor) {
            return
        }
        editor.caretModel.addCaretListener(this)
        this.editor?.caretModel?.removeCaretListener(this)
        this.editor = editor
    }
}