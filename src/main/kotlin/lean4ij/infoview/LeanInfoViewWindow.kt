package lean4ij.infoview

import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FoldingListener
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lean4ij.lsp.data.InfoviewRender
import lean4ij.lsp.data.InteractiveDiagnostics
import lean4ij.lsp.data.InteractiveGoals
import lean4ij.lsp.data.InteractiveTermGoal
import lean4ij.project.LeanProjectService
import javax.swing.BorderFactory
import javax.swing.JEditorPane

/**
 * check :https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html#ui-dsl-basics
 * for some cleaner way to write ui stuff
 */
class LeanInfoViewWindow(val toolWindow: ToolWindow) : SimpleToolWindowPanel(true) {
    private val goals = JEditorPane()
    val editor : CompletableDeferred<EditorEx> = CompletableDeferred()

    /**
     * This si for displaying popup expr
     */
    val popupEditor : CompletableDeferred<EditorEx> = CompletableDeferred()
    val project = toolWindow.project
    val leanProject = project.service<LeanProjectService>()
    init {
        leanProject.scope.launch(Dispatchers.EDT) {
            try {
                editor.complete(createEditor())
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
     * check the following links about thread and ui
     * - https://intellij-support.jetbrains.com/hc/en-us/community/posts/360009458040-Error-writing-data-in-a-tree-provided-by-a-background-thread
     * - https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html
     * Once it's not update but now the method revalidate and updateUI seem not required now.
     */
    fun updateGoal(goal: String) {
        leanProject.scope.launch {
            editor.await().document.setText(goal)
            goals.text = goal
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
            setHorizontalScrollbarVisible(false)
            setVerticalScrollbarVisible(true)
            isRendererMode = true
        }
        return editor
    }

    suspend fun updateDirectText(text: String) {
        val editorEx: EditorEx = editor.await()
        editorEx.document.setText(text)
        editorEx.component.repaint()
    }

    private var mouseMotionListener : EditorMouseMotionListener? = null
    /**
     *  // TODO this should add some UT for the rendering
     */
    suspend fun updateEditorMouseMotionListener(
        interactiveInfo: InfoviewRender,
        file: VirtualFile,
        logicalPosition: LogicalPosition,
        interactiveGoals: InteractiveGoals?,
        interactiveTermGoal: InteractiveTermGoal?,
        interactiveDiagnostics: List<InteractiveDiagnostics>?,
        interactiveDiagnosticsAllMessages: List<InteractiveDiagnostics>?
    ) {
        val editorEx: EditorEx = editor.await()
        editorEx.markupModel.removeAllHighlighters()
        editorEx.document.setText(interactiveInfo.toString())

        // TODO maybe a configuration for this
        // always move to the beginning while update goal, to avoid losing focus while all message expanded
        // TODO nevertheless there maybe some better way
        editorEx.caretModel.moveToOffset(0)
        editorEx.scrollingModel.scrollToCaret(ScrollType.CENTER)

        editorEx.foldingModel.runBatchFoldingOperation {
            editorEx.foldingModel.clearFoldRegions()
            var allMessagesFoldRegion : FoldRegion? = null
            for (folding in interactiveInfo.foldings) {
                val foldRegion = editorEx.foldingModel.addFoldRegion(folding.startOffset, folding.endOffset, folding.placeholderText)
                foldRegion?.isExpanded = folding.expanded
                if (folding.isAllMessages) {
                    allMessagesFoldRegion = foldRegion
                }
            }
            editorEx.foldingModel.addListener(object : FoldingListener {
                override fun onFoldRegionStateChange(region: FoldRegion) {
                    if (allMessagesFoldRegion == region) {
                        LeanInfoViewWindowFactory.expandAllMessage = region.isExpanded
                    }
                }
            }) {
                // TODO should some disposal add here?
            }
        }
        // highlights
        for (highlight in interactiveInfo.highlights) {
            editorEx.markupModel.addRangeHighlighter(highlight.startOffset, highlight.endOffset, HighlighterLayer.SYNTAX, highlight.textAttributes, HighlighterTargetArea.EXACT_RANGE)
        }


        val support = EditorHyperlinkSupport.get(editorEx)
        setContent(editorEx.component)
        // TODO does it require new object for each update?
        //      it seems so, otherwise the hyperlinks seems mixed and requires remove
        //      but still, maybe only one is better, try to remove old hyperlinks
        //      check if multiple editors would leak or not
        if (mouseMotionListener != null) {
            editorEx.removeEditorMouseMotionListener(mouseMotionListener!!)
        }
        mouseMotionListener = InfoviewMouseMotionListener(leanProject, this, editorEx, file, logicalPosition,
            interactiveGoals, interactiveTermGoal, interactiveDiagnostics, interactiveDiagnosticsAllMessages)
        editorEx.addEditorMouseMotionListener(mouseMotionListener!!)
    }

}