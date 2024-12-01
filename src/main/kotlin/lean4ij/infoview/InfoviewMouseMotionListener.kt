package lean4ij.infoview

import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import lean4ij.lsp.data.ContextInfo
import lean4ij.lsp.data.InteractiveDiagnostics
import lean4ij.lsp.data.InteractiveGoals
import lean4ij.lsp.data.InteractiveTermGoal
import lean4ij.lsp.data.Position
import lean4ij.project.LeanProjectService
import lean4ij.setting.Lean4Settings
import java.awt.Color

/**
 * TODO this class should require some refactor
 */
class InfoviewMouseMotionListener(
    private val leanProjectService: LeanProjectService,
    private val infoViewWindow: LeanInfoViewWindow,
    private val editor: EditorEx,
    private val file: VirtualFile,
    private val position: Position,
    private val interactiveGoals: InteractiveGoals?,
    private val interactiveTermGoal: InteractiveTermGoal?,
    private val interactiveDiagnostics: List<InteractiveDiagnostics>?,
    private val interactiveDiagnosticsAllMessages: List<InteractiveDiagnostics>?
) : EditorMouseMotionListener {
    private val lean4Settings = service<Lean4Settings>()
    private var hyperLink: RangeHighlighter? = null
    private val support = EditorHyperlinkSupport.get(editor)
    override fun mouseMoved(e: EditorMouseEvent) {
        if (hyperLink != null) {
            support.removeHyperlink(hyperLink!!)
        }
        if (!e.isOverText) {
            leanProjectService.scope.launch {
                offsetsFlow.send(null)
            }
            return
        }
        var c: Triple<ContextInfo, Int, Int>? = null
        if (interactiveGoals != null) {
            c = interactiveGoals.getCodeText(e.offset)
        }
        if (c == null && interactiveTermGoal != null) {
            c = interactiveTermGoal.getCodeText(e.offset)
        }
        if (c == null && interactiveDiagnostics != null) {
            for (diagnostic in interactiveDiagnostics) {
                c = diagnostic.message.getCodeText(e.offset, null)
                if (c != null) {
                    break
                }
            }
        }
        if (c == null && interactiveDiagnosticsAllMessages != null) {
            for (diagnostic in interactiveDiagnosticsAllMessages) {
                // TODO why passing null?
                c = diagnostic.message.getCodeText(e.offset, null)
                if (c != null) {
                    break
                }
            }
        }
        if (c == null) {
            leanProjectService.scope.launch {
                offsetsFlow.send(null)
            }
            return
        }

        // TODO maybe make this singleton?
        val attr = object : TextAttributes() {
            override fun getBackgroundColor(): Color {
                // TODO document this
                // TODO should scheme be cache?
                val scheme = EditorColorsManager.getInstance().globalScheme
                // TODO customize attr? or would backgroundColor null?
                //      indeed here it can be null, don't know why Kotlin does not mark it as error
                // TODO there is cases here the background of identifier under current caret is null
                // TODO do this better in a way
                var color = scheme.getAttributes(EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES).backgroundColor
                if (color != null) {
                    return color
                }
                color = scheme.getColor(EditorColors.CARET_COLOR)
                if (color != null) {
                    return color
                }
                return scheme.defaultBackground
            }
        }
        createPopup(c, attr)
        leanProjectService.scope.launch {
            offsetsFlow.send(
                InfoviewPopupDocumentation(
                    leanProjectService.scope, infoViewWindow, file, position, c.first,
                    RelativePoint(e.mouseEvent)
                )
            )
        }
    }

    /**
     * this is some kind copy from [com.intellij.execution.impl.EditorHyperlinkSupport.createHyperlink]
     */
    private fun createPopup(c: Triple<ContextInfo, Int, Int>, attr: TextAttributes) {
        hyperLink = editor.markupModel.addRangeHighlighterAndChangeAttributes(
            CodeInsightColors.HYPERLINK_ATTRIBUTES,
            c.second,
            c.third,
            HighlighterLayer.HYPERLINK,
            HighlighterTargetArea.EXACT_RANGE,
            false
        ) { ex ->
            ex.textAttributes = attr
        }
    }

    private var offsetsFlow = Channel<InfoviewPopupDocumentation?>()

    init {
        leanProjectService.scope.launch {
            tryEmitHover()
        }
    }

    private suspend fun tryEmitHover() {
        var oldHovering: InfoviewPopupDocumentation? = null
        var hovering: InfoviewPopupDocumentation? = null
        // TODO is it OK here using infinite loop?
        //      should it be some disposal behavior?
        while (true) {
            try {
                // TODO the time control here seems problematic
                //      it seems longer than the setting
                hovering = withTimeout(lean4Settings.hoveringTimeBeforePopupNativeInfoviewDoc.toLong()) {
                    offsetsFlow.receive()
                }
                if (oldHovering != null && oldHovering.contextInfo != hovering?.contextInfo) {
                    oldHovering.cancel()
                    oldHovering = null
                }
            } catch (e: TimeoutCancellationException) {
                if (hovering != null && hovering != oldHovering && hovering.contextInfo != oldHovering?.contextInfo) {
                    oldHovering?.cancel()
                    oldHovering = hovering
                    hovering.navigate(leanProjectService.project)
                }
            }
        }
    }

}