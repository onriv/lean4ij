package lean4ij.infoview

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lean4ij.infoview.dsl.InfoObjectModel
import lean4ij.infoview.dsl.info
import lean4ij.lsp.data.InteractiveGoals
import lean4ij.lsp.data.InteractiveTermGoal
import lean4ij.lsp.data.Position
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

@Service(Service.Level.PROJECT)
class MiniInfoviewService(private val project: Project, val scope: CoroutineScope) {

    companion object {
        const val ALLOW_TERM_GOALS: Boolean = false
    }

    var showing = false
    private var isScrolling = false
    private var scrollJob: Job? = null
    private var currentEditor: Editor? = null
    private var areaListener: VisibleAreaListener? = null

    var lastContent: InfoObjectModel? = null
    var currentPopover: JBPopup? = null
    var miniInfoview: MiniInfoview? = null

    private fun cancel() {
        scope.launch(Dispatchers.EDT) {
            removeListeners()
            currentPopover?.cancel()
            currentPopover = null
            miniInfoview = null
        }
    }

    private fun createPopover(editor: Editor?, position: Position?) {
        if (editor == null || position == null) return

        val factory = JBPopupFactory.getInstance()
        miniInfoview = MiniInfoview(project)
        val jPanel = JPanel(VerticalLayout(1))
        jPanel.add(miniInfoview)

        val popup = JBScrollPane(jPanel)
        popup.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        popup.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED

        currentPopover = factory.createComponentPopupBuilder(popup, null)
            .setResizable(true)
            .setFocusable(false)
            .setMovable(true)
            .createPopup()

        setupListeners(editor)
    }

    private fun showAtCursor(editor: Editor, position: Position) {
        val visualPosition = editor.offsetToVisualPosition(editor.logicalPositionToOffset(
            LogicalPosition(position.line, position.character)
        ))

        val point = editor.visualPositionToXY(visualPosition)

        point.y += editor.lineHeight

        val relativePoint = RelativePoint(editor.contentComponent, point)

        if (currentPopover?.canShow() != false) {
            currentPopover?.show(relativePoint)
        }
        else {
            currentPopover?.setLocation(relativePoint.screenPoint)
        }
    }

    private fun setupListeners(editor: Editor) {
        removeListeners()

        areaListener = object : VisibleAreaListener {
            override fun visibleAreaChanged(e: VisibleAreaEvent) {
                if (!showing) return

                if (!isScrolling) {
                    isScrolling = true
                    cancel();
                }

                scrollJob?.cancel()
                scrollJob = scope.launch {
                    delay(500)
                    isScrolling = false
                    if (showing && lastContent != null) {
                        val caretPosition = editor.caretModel.logicalPosition
                        val position = Position(caretPosition.line, caretPosition.column)
                        createOrUpdatePopupPanel(lastContent, editor, position)
                    }
                }
            }
        }

        // Register listeners
        editor.scrollingModel.addVisibleAreaListener(areaListener!!)
    }

    private fun removeListeners() {
        currentEditor?.let { editor ->
            areaListener?.let { editor.scrollingModel.removeVisibleAreaListener(it) }
        }
        areaListener = null
    }

    private fun displayContent(content: InfoObjectModel, editor: Editor, position: Position) {
        scope.launch(Dispatchers.EDT) {
            if (currentEditor != editor || currentPopover?.isVisible != true || miniInfoview == null) {
                createPopover(editor, position)
            }

            // Update the existing editor content
            miniInfoview?.let { view ->
                val editor = view.getEditor()
                editor.markupModel.removeAllHighlighters()
                content.output(view.getEditor())

                currentPopover?.size = view.measureIntrinsicContentSize()
            }

            showAtCursor(editor, position)
        }
    }

    private fun createOrUpdatePopupPanel(doc: InfoObjectModel?, editor: Editor?, position: Position?) {
        lastContent = doc
        if (showing && lastContent != null && editor != null && position != null) {
            displayContent(lastContent!!, editor, position)
        } else {
            cancel()
        }
    }

    private fun getGoal(interactiveGoals: InteractiveGoals?, interactiveTermGoal: InteractiveTermGoal?): InfoObjectModel? {
        val goals = interactiveGoals?.goals
        val prefix = "⊢ "

        val type = if (goals?.size == 1) {
            interactiveGoals.goals[0].type
        } else if (ALLOW_TERM_GOALS) {
            interactiveTermGoal?.type ?: return null
        } else {
            return null
        }

        return info {
            p(prefix, Lean4TextAttributesKeys.SwingInfoviewGoalSymbol)
            add(type.toInfoObjectModel())
        }
    }

    fun updateCaret(
        editor: Editor,
        position: Position,
        interactiveGoals: InteractiveGoals?,
        interactiveTermGoal: InteractiveTermGoal?,
    ) {
        lastContent = getGoal(interactiveGoals, interactiveTermGoal)
        createOrUpdatePopupPanel(lastContent, editor, position)
        // sometimes necessary for cacheing so toggle visibility can work
        currentEditor = editor
    }

    fun toggleVisibility() {
        showing = !showing
        if (showing && lastContent != null && currentEditor != null) {
            val caretPosition = currentEditor!!.caretModel.logicalPosition
            val position = Position(caretPosition.line, caretPosition.column)
            createOrUpdatePopupPanel(lastContent, currentEditor, position)
        } else {
            cancel()
        }
    }
}