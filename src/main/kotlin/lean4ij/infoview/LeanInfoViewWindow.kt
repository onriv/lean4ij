package lean4ij.infoview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lean4ij.project.LeanProjectService
import javax.swing.BorderFactory

/**
 * check :https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html#ui-dsl-basics
 * for some cleaner way to write ui stuff
 */
class LeanInfoViewWindow(val toolWindow: ToolWindow) : SimpleToolWindowPanel(true) {
    /**
     * TODO make this private
     */
    private val editor : CompletableDeferred<EditorEx> = CompletableDeferred()

    suspend fun getEditor(): EditorEx {
        return editor.await()
    }

    /**
     * This si for displaying popup expr
     */
    val popupEditor : CompletableDeferred<EditorEx> = CompletableDeferred()
    val project = toolWindow.project
    val leanProject = project.service<LeanProjectService>()
    val leanInfoviewService = project.service<LeanInfoviewService>()
    init {
        leanProject.scope.launch(Dispatchers.EDT) {
            try {
                val editor0 = createEditor()
                editor0.contextMenuGroupId = "lean4ij.infoview.rightClickGroup"
                editor0.installPopupHandler { event ->
                    val leanInfoviewService = project.service<LeanInfoviewService>()
                    leanInfoviewService.caretIsOverText = event.isOverText
                    // if the event is not over text, we do not pass it to context menu group
                    !event.isOverText
                }

                editor.complete(editor0)
            } catch (ex: Throwable) {
                // TODO should here log?
                editor.completeExceptionally(ex)
            }
            try {
                popupEditor.complete(createEditor(true))
            } catch (ex: Throwable) {
                // TODO should here log?
                popupEditor.completeExceptionally(ex)
            }
        }
        leanInfoviewService.toolWindow = this
    }

    private val BORDER = BorderFactory.createEmptyBorder(3, 0, 5, 0)

    // TODO
    private fun render(map: Map<*, *>): String {
        for (g in map["goals"] as List<*>) {
            return ""
        }
        return ""
    }

    /**
     * create an editorEx for rendering the info view
     * **this is only for EDT**, create it using
     */
    private fun createEditor(isPopupDoc: Boolean=false): EditorEx {
        val editor = EditorFactory.getInstance()
            // java.lang.RuntimeException: Memory leak detected: 'com.intellij.openapi.editor.impl.view.EditorView@601dc681' (class com.intellij.openapi.editor.impl.view.EditorView) was registered in Disposer as a child of 'ROOT_DISPOSABLE' (class com.intellij.openapi.util.Disposer$2) but wasn't disposed.
            // Register it with a proper parentDisposable or ensure that it's always disposed by direct Disposer.dispose call.
            // See https://jetbrains.org/intellij/sdk/docs/basics/disposers.html for more details.
            // The corresponding Disposer.register() stacktrace is shown as the cause:
            .createViewer(DocumentImpl(" ", true), toolWindow.project) as EditorEx
        // val editor = editorTextField.getEditor(true)!!
        with (editor) {
            with (settings) {
                isRightMarginShown = false
                isLineNumbersShown = false
                isLineMarkerAreaShown = false
                isRefrainFromScrolling = true
                isCaretRowShown = true
                isBlinkCaret = true
                isUseSoftWraps = true
                setGutterIconsShown(false)
                additionalLinesCount = 0
                additionalColumnsCount = 1
                isVirtualSpace = false
                if (isPopupDoc) {
                    // for popup doc of inline infoview, the folding outline should not be shown
                    isFoldingOutlineShown = false
                } else {
                    isFoldingOutlineShown = true
                }
            }
            headerComponent = null
            setCaretEnabled(true)
            // if true, then it's in fact also only visible if necessary
            // check com.intellij.openapi.editor.impl.EditorImpl#setHorizontalScrollbarVisible
            setHorizontalScrollbarVisible(true)
            setVerticalScrollbarVisible(true)
            isRendererMode = false
        }
        return editor
    }

    suspend fun updateDirectText(text: String) {
        val editorEx: EditorEx = editor.await()
        ApplicationManager.getApplication().invokeLater {
            editorEx.document.setText(text)
            editorEx.component.repaint()
        }
    }

    /**
     * // TODO this should add some UT for the rendering
     * TODO this in fact can be static
     */
    fun updateEditorMouseMotionListenerV1(
        context: LeanInfoviewContext
    ) {
        val editorEx: EditorEx = context.infoviewEditor
        editorEx.markupModel.removeAllHighlighters()
        context.rootObjectModel.output(editorEx)

        // this is the critical statement for showing content
        setContent(editorEx.component)

        // TODO maybe a configuration for this
        // always move to the beginning while update goal, to avoid losing focus while all message expanded
        // TODO nevertheless there maybe some better way
        editorEx.caretModel.moveToOffset(0)
        editorEx.scrollingModel.scrollToCaret(ScrollType.CENTER)

        // TODO does it require new object for each update?
        //      it seems so, otherwise the hyperlinks seems mixed and requires remove
        //      but still, maybe only one is better, try to remove old hyperlinks
        //      check if multiple editors would leak or not
        if (mouseMotionListenerV1 != null) {
            editorEx.removeEditorMouseMotionListener(mouseMotionListenerV1!!)
        }
        // TODO this can be refactored to the InfoviewRender class, in that way the definition of hovering
        //      can be done in the same time when the rendering is defined
        mouseMotionListenerV1 = InfoviewMouseMotionListenerV1(context)
        editorEx.addEditorMouseMotionListener(mouseMotionListenerV1!!)

        // mouse listener
        if (mouseListenerV1 != null) {
            editorEx.removeEditorMouseListener(mouseListenerV1!!)
        }
        mouseListenerV1 = InfoviewMouseListenerV1(context)
        editorEx.addEditorMouseListener(mouseListenerV1!!)
    }

    private var mouseMotionListenerV1 : EditorMouseMotionListener? = null

    private var mouseListenerV1 : EditorMouseListener? = null

    fun restartEditor() {
        leanProject.scope.launch(Dispatchers.EDT) {
            editor.complete(createEditor())
        }
    }

}