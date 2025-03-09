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

                installPopupHandler(editor0)

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
     * This creates a right-click popup menu for the infoview editor
     * There are many ways to do this,
     * one is ths contextMenuGroup, or handle it manually with installPopupHandler
     * or using things like PopupHandler.installPopupMenu
     * check: https://intellij-support.jetbrains.com/hc/en-us/community/posts/6912597187218-How-can-I-create-a-custom-right-click-menu-that-contains-a-couple-of-actions-and-attach-the-menu-to-each-element-in-a-JTree-in-my-plugin
     * and https://github.com/JetBrains/intellij-community/blob/master/platform/lang-impl/src/com/intellij/packageDependencies/ui/DependenciesPanel.java
     */
    private fun installPopupHandler(editor: EditorEx) {
        editor.contextMenuGroupId = "lean4ij.infoview.rightClickGroup"
        editor.installPopupHandler { event ->
            val leanInfoviewService = project.service<LeanInfoviewService>()
            leanInfoviewService.caretIsOverText = event.isOverText
            // if the event is not over text, we do not pass it to context menu group
            !event.isOverText
        }
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
                // for popup doc of inline infoview, the folding outline should not be shown
                isFoldingOutlineShown = !isPopupDoc
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
    fun updateEditorMouseMotionListener(
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
        if (mouseMotionListener != null) {
            editorEx.removeEditorMouseMotionListener(mouseMotionListener!!)
        }
        // TODO this can be refactored to the InfoviewRender class, in that way the definition of hovering
        //      can be done in the same time when the rendering is defined
        mouseMotionListener = InfoviewMouseMotionListener(context)
        editorEx.addEditorMouseMotionListener(mouseMotionListener!!)

        // mouse listener
        if (mouseListener != null) {
            editorEx.removeEditorMouseListener(mouseListener!!)
        }
        mouseListener = InfoviewMouseListener(context)
        editorEx.addEditorMouseListener(mouseListener!!)
    }

    private var mouseMotionListener : EditorMouseMotionListener? = null

    private var mouseListener : EditorMouseListener? = null

    fun restartEditor() {
        leanProject.scope.launch(Dispatchers.EDT) {
            editor.complete(createEditor())
        }
    }

}