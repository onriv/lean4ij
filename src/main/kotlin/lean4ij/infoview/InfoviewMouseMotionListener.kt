package lean4ij.infoview

import com.google.rpc.Code
import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withTimeout
import lean4ij.lsp.data.*
import lean4ij.project.BuildEvent
import lean4ij.project.LeanProjectService
import java.awt.Color

/**
 * TODO this class should require some refactor
 */
class InfoviewMouseMotionListener(
    private val leanProjectService: LeanProjectService,
    private val infoViewWindow: LeanInfoViewWindow,
    private val support: EditorHyperlinkSupport,
    private val file: VirtualFile,
    private val logicalPosition: LogicalPosition,
    private val interactiveGoals: InteractiveGoals?,
    private val interactiveTermGoal: InteractiveTermGoal?,
    private val interactiveDiagnostics: List<InteractiveDiagnostics>?,
    private val interactiveDiagnosticsAllMessages: List<InteractiveDiagnostics>?
) : EditorMouseMotionListener {
    private var hyperLink: RangeHighlighter? = null
    override fun mouseMoved(e: EditorMouseEvent) {
        emitOffset(e.offset)
        if (hyperLink != null) {
            support.removeHyperlink(hyperLink!!)
        }
        if (!e.isOverText) {
            return
        }
        var c : Triple<ContextInfo, Int, Int>? = null
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
            return
        }

        hyperLink = support.createHyperlink(
            c.second,
            c.third,
            object : TextAttributes() {
                override fun getBackgroundColor(): Color {
                    // TODO document this
                    // TODO should scheme be cache?
                    val scheme = EditorColorsManager.getInstance().globalScheme
                    // TODO customize attr? or would backgroundColor null?
                    //      indeed here it can be null, don't know why Kotlin does not mark it as error
                    return scheme.getAttributes(EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES).backgroundColor
                }
            },
            CodeWithInfosDocumentationHyperLink(leanProjectService.scope, infoViewWindow, file, logicalPosition, c.first,
                RelativePoint(e.mouseEvent) )
        )
    }

    private var offsetsFlow = Channel<Int>()

    private fun emitOffset(offset: Int) {
        leanProjectService.scope.launch {
            offsetsFlow.send(offset)
        }
    }

    init {
        leanProjectService.scope.launch {
            tryEmitHover()
        }
    }

    private suspend fun tryEmitHover() {
        var oldHovering: CodeWithInfosDocumentationHyperLink? = null
        var isHovering = false
        var oldOffset = -1
        var offset = -1
        // TODO is it OK here using infinite loop?
        //      should it be some disposal behavior?
        while (true) {
            try {
                // TODO the time control here seems problematic
                //      it seems longer than the setting
                offset = withTimeout(200) {
                    offsetsFlow.receive()
                }
                isHovering = false
                oldHovering?.cancel()
            } catch (e: TimeoutCancellationException) {
                if (offset == oldOffset && oldOffset != -1 && !isHovering) {
                    isHovering = true
                    val hyperLink = support.getHyperlinkAt(offset)
                    if (hyperLink != null) {
                        oldHovering?.cancel()
                        oldHovering = hyperLink as CodeWithInfosDocumentationHyperLink
                        oldHovering.navigate(leanProjectService.project)
                    }
                }
                oldOffset = offset
            }
        }
    }

}