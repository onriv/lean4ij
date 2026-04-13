package lean4ij.infoview

import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.awt.RelativePoint
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import lean4ij.lsp.data.ContextInfo
import lean4ij.setting.Lean4Settings
import java.awt.Color

/**
 * Handles mouse hover events in the internal infoview editor.
 *
 * When the user hovers over a term in the infoview, this listener:
 * 1. Looks up the [ContextInfo] for the hovered offset via [InfoObjectModel.getCodeContext]
 * 2. Highlights the hovered term with a hyperlink-style background color
 * 3. After a configurable delay ([Lean4Settings.hoveringTimeBeforePopupNativeInfoviewDoc]),
 *    triggers an [InfoviewPopupDocumentation] popup showing the term's type and documentation
 *
 * ## Debounce Logic
 * Uses a [Channel]-based approach: each mouse move sends the current hover target to a
 * processing coroutine. The coroutine uses `withTimeout` to implement the hover delay.
 * If the mouse moves to a different term before the timeout, the previous popup is cancelled.
 * If the mouse stays on the same term past the timeout, the popup is shown.
 *
 * ## Hyperlink Highlighting
 * Creates a temporary [RangeHighlighter] with the IDE's identifier-under-caret background
 * color to visually indicate the hovered term. The highlight is removed when the mouse
 * moves away.
 *
 * @param context The infoview context containing the rendered content and project references
 */
class InfoviewMouseMotionListener(val context: LeanInfoviewContext) : EditorMouseMotionListener {

    private val lean4Settings = service<Lean4Settings>()
    private val offsetsFlow = Channel<InfoviewPopupDocumentation?>()
    private var hyperLink: RangeHighlighter? = null
    private val support = EditorHyperlinkSupport.get(context.infoviewEditor)

    init {
        context.leanProject.scope.launch {
            tryEmitHover()
        }
    }

    private val leanInfoviewService
        get() = context.leanProject.project.service<LeanInfoviewService>()

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
                    hovering.navigate(context.leanProject.project)
                }
            }
        }
    }
    override fun mouseMoved(e: EditorMouseEvent) {
        if (hyperLink != null) {
            support.removeHyperlink(hyperLink!!)
        }
        if (!e.isOverText) {
            context.leanProject.scope.launch {
                offsetsFlow.send(null)
                leanInfoviewService.contextInfo = null
            }
            return
        }
        val c : Triple<ContextInfo, Int, Int>? = context.rootObjectModel.getCodeContext(e.offset)
        if (c == null) {
            context.leanProject.scope.launch {
                offsetsFlow.send(null)
                leanInfoviewService.contextInfo = null
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
        context.leanProject.scope.launch {
            leanInfoviewService.contextInfo = Triple(c.first, context.file, context.position)
            offsetsFlow.send(
                // TODO move argument of popup document to context too
                InfoviewPopupDocumentation(
                    context.leanProject.scope, context.leanInfoViewWindow, context.file, context.position, c.first,
                    RelativePoint(e.mouseEvent)
                )
            )
        }
    }

    /**
     * this is some kind copy from [com.intellij.execution.impl.EditorHyperlinkSupport.createHyperlink]
     */
    private fun createPopup(c: Triple<ContextInfo, Int, Int>, attr: TextAttributes) {
        hyperLink = context.infoviewEditor.markupModel.addRangeHighlighterAndChangeAttributes(
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
}