package lean4ij.infoview

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.OnePixelDivider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lean4ij.infoview.dsl.info
import lean4ij.lsp.data.FoldingData
import lean4ij.lsp.data.InfoviewRender
import lean4ij.lsp.data.InteractiveDiagnostics
import lean4ij.lsp.data.InteractiveGoals
import lean4ij.lsp.data.InteractiveTermGoal
import lean4ij.lsp.data.MsgUnsupported
import lean4ij.lsp.data.Position
import lean4ij.lsp.data.TaggedTextTag
import lean4ij.project.LeanProjectService
import java.awt.BorderLayout


/**
 * TODO do not show this if indexing, the doc seems saying it's an option for it
 *      maybe dumb-aware or something
 */
class LeanInfoViewWindowFactory : ToolWindowFactory {

    companion object {

        /**
         * The id is from plugin.xml
         */
        fun getLeanInfoview(project: Project): LeanInfoViewWindow? {
            val contents = ToolWindowManager.getInstance(project)
                .getToolWindow("LeanInfoViewWindow")!!
                .contentManager.contents
            if (contents.isEmpty()) {
                return null
            }
            return contents[0].component as LeanInfoViewWindow
        }

        fun createInfoObjectModel(
            file: VirtualFile,
            position: Position,
            interactiveGoals: InteractiveGoals?,
            interactiveTermGoal: InteractiveTermGoal?,
            interactiveDiagnostics: List<InteractiveDiagnostics>?,
            allMessage: List<InteractiveDiagnostics>?
        ) = info {
            fold {
                var size = 0
                h1("${file.name}:${position.line + 1}:${position.character}")
                interactiveGoals?.toInfoObjectModel()?.let {
                    add(it)
                    size++
                }
                interactiveTermGoal?.toInfoObjectModel()?.let {
                    // if it has interactive goals, then there should be some line break
                    // for interactive term goal, i.e., expected type
                    if (size > 0) {
                        br()
                    }
                    add(it)
                    size++
                }
                if (interactiveDiagnostics?.isNotEmpty() == true) {
                    if (size > 0) {
                        br()
                    }
                    fold {
                        h2("Messages (${interactiveDiagnostics.size})")
                        for ((idx, i) in interactiveDiagnostics.withIndex()) {
                            fold {
                                h3("${file.name}:${i.fullRange.start.line + 1}:${i.fullRange.start.character}")
                                add(i.toInfoObjectModel())
                                size++
                                if (idx != interactiveDiagnostics.lastIndex) {
                                    br()
                                }
                            }
                        }
                    }
                }
                if (size == 0) {
                    +"No info found."
                }
            }
            if (!allMessage.isNullOrEmpty()) {
                br()
                fold(
                    expanded = expandAllMessage,
                    isAllMessages = true,
                    listener = {
                        expandAllMessage = it.isExpanded
                    }
                ) {
                    h1("All Messages (${allMessage.size})")
                    for ((idx, i) in allMessage.withIndex()) {
                        fold {
                            h2("${file.name}:${i.fullRange.start.line + 1}:${i.fullRange.start.character}")
                            add(i.toInfoObjectModel())
                        }
                        if (idx != allMessage.lastIndex) {
                            br()
                        }
                    }
                }
            }
        }

        /**
         * TODO the implementation should absolutely be replaced by better rendering way
         *      using raw text it's very inconvenient to update things like hovering event
         *      but though vim/emacs has to do it this way maybe ...
         * TODO passing things like editor etc seems cumbersome, maybe add some implement for context
         * @param editor Editor that is currently selected. It's not the infoview tool window editor.
         */
        fun updateInteractiveGoalV1(
            editor: Editor,
            project: Project,
            file: VirtualFile?, // TODO this should add some UT for the rendering
            position: Position,
            interactiveGoals: InteractiveGoals?,
            interactiveTermGoal: InteractiveTermGoal?,
            interactiveDiagnostics: List<InteractiveDiagnostics>?,
            allMessage: List<InteractiveDiagnostics>?
        ) {
            if (file == null) {
                return
            }
            val infoViewWindow = getLeanInfoview(project) ?: return

            val infoObjectModel = createInfoObjectModel(file, position, interactiveGoals, interactiveTermGoal, interactiveDiagnostics, allMessage)

            // TODO render message
            // TODO this seems kind of should be put inside rendering, check how to do this
            // TODO maybe it's too broad, maybe only createEditor in EDT
            var leanProjectService = project.service<LeanProjectService>()
            val scope = leanProjectService.scope
            // The scope.launch here is mainly for the editor
            // ref: https://plugins.jetbrains.com/docs/intellij/coroutine-tips-and-tricks.html
            // TODO minimize the invoke later range
            scope.launch(Dispatchers.EDT) {
                val context = LeanInfoviewContext(leanProjectService, infoViewWindow, infoViewWindow.getEditor(), file, position, infoObjectModel)
                infoViewWindow.updateEditorMouseMotionListenerV1(context)
            }
        }

        /**
         * TODO should this be a setting?
         */
        var expandAllMessage: Boolean = false

        /**
         * TODO the implementation should absolutely be replaced by better rendering way
         *      using raw text it's very inconvenient to update things like hovering event
         *      but though vim/emacs has to do it this way maybe ...
         * TODO passing things like editor etc seems cumbersome, maybe add some implement for context
         */
        fun updateInteractiveGoal(
            editor: Editor,
            project: Project,
            file: VirtualFile?, // TODO this should add some UT for the rendering
            position: Position,
            interactiveGoals: InteractiveGoals?,
            interactiveTermGoal: InteractiveTermGoal?,
            interactiveDiagnostics: List<InteractiveDiagnostics>?,
            allMessage: List<InteractiveDiagnostics>?
        ) {
            if (file == null) {
                return
            }
            val infoViewWindow = getLeanInfoview(project) ?: return
            // TODO implement the fold/open logic
            val infoviewRender = InfoviewRender(project, file)
            val start = infoviewRender.length
            val header = "${file.name}:${position.line + 1}:${position.character}"
            infoviewRender.append("${header}")
            infoviewRender.highlight(
                start,
                infoviewRender.length,
                EditorColorsManager.getInstance().globalScheme.getAttributes(Lean4TextAttributesKeys.SwingInfoviewCurrentPosition.key)
            )
            infoviewRender.append('\n')
            // TODO here maybe null?
            // TODO refactor this
            if (interactiveGoals != null || interactiveTermGoal != null || !interactiveDiagnostics.isNullOrEmpty()) {
                interactiveGoals?.toInfoViewString(infoviewRender)
                interactiveTermGoal?.toInfoViewString(editor, infoviewRender)
                if (!interactiveDiagnostics.isNullOrEmpty()) {
                    val header = "Messages (${interactiveDiagnostics.size})"
                    val start = infoviewRender.length
                    infoviewRender.append("${header}\n")
                    interactiveDiagnostics.forEach { i ->
                        infoviewRender.append("${file.name}:${i.fullRange.start.line + 1}:${i.fullRange.start.character}\n")
                        i.toInfoViewString(infoviewRender)
                        infoviewRender.append('\n')
                    }
                    // The last line break should not be folded, here the impl seems kind of adhoc
                    infoviewRender.deleteLastChar()
                    val end = infoviewRender.length
                    infoviewRender.addFoldingOperation(start, end, header)
                    infoviewRender.append('\n')
                }
            } else {
                infoviewRender.append("No info found.\n")
            }
            // The last line break should not be folded, here the impl seems kind of adhoc
            infoviewRender.deleteLastChar()
            val end = infoviewRender.length
            infoviewRender.addFoldingOperation(start, end, header)
            infoviewRender.append("\n")

            if (!allMessage.isNullOrEmpty()) {
                val header = "All Messages (${allMessage.size})"
                val start = infoviewRender.length
                infoviewRender.append("$header\n")
                allMessage.forEach { i ->
                    val header = "${file.name}:${i.fullRange.start.line + 1}:${i.fullRange.start.character}"
                    val start = infoviewRender.length
                    infoviewRender.append(header)
                    val end1 = infoviewRender.length
                    infoviewRender.append('\n')
                    // TODO better way than class check?
                    if (i.message is TaggedTextTag) {
                        // TODO rather than using string contains in the following, move highlight logic into
                        //      the toInfoViewString method
                        val content = i.message.toInfoViewString(infoviewRender, null)
                        if (i.message.f0 is MsgUnsupported) {
                            infoviewRender.highlight(
                                start,
                                end1,
                                Lean4TextAttributesKeys.SwingInfoviewAllMessageUnsupportedPos
                            )
                        } else if (content.contains("declaration uses 'sorry'")) {
                            infoviewRender.highlight(
                                start,
                                end1,
                                Lean4TextAttributesKeys.SwingInfoviewAllMessageSorryPos
                            )
                        } else {
                            infoviewRender.highlight(start, end1, Lean4TextAttributesKeys.SwingInfoviewAllMessagePos)
                        }
                    } else {
                        // TODO for TaggedTextAppend there is also case for not supported trace
                        val content = i.message.toInfoViewString(infoviewRender, null)
                        // TODO remove this magic number and define some enum for it
                        //      instance : ToJson DiagnosticSeverity := ⟨fun
                        //      | DiagnosticSeverity.error       => 1
                        //      | DiagnosticSeverity.warning     => 2
                        //      | DiagnosticSeverity.information => 3
                        //      | DiagnosticSeverity.hint        => 4⟩
                        //  check src/Lean/Data/Lsp/Diagnostics.lean
                        var key = if (i.severity == 1) {
                            Lean4TextAttributesKeys.SwingInfoviewAllMessageErrorPos
                        } else {
                            Lean4TextAttributesKeys.SwingInfoviewAllMessagePos
                        }
                        infoviewRender.highlight(start, end1, key)
                    }
                    val end2 = infoviewRender.length
                    infoviewRender.addFoldingOperation(start, end2, header)
                    infoviewRender.append('\n')
                }
                infoviewRender.deleteLastChar()
                val end = infoviewRender.length
                infoviewRender.addFoldingOperation(
                    FoldingData(
                        start,
                        end,
                        header,
                        expandAllMessage,
                        isAllMessages = true
                    )
                )
                infoviewRender.append("\n")
            } else {
                val header = "All Messages (0)"
                val start = infoviewRender.length
                infoviewRender.append("$header\n")
                infoviewRender.append("No messages")
                infoviewRender.addFoldingOperation(start, infoviewRender.length, header)
                infoviewRender.append('\n')
            }

            // TODO render message
            // TODO this seems kind of should be put inside rendering, check how to do this
            // TODO maybe it's too broad, maybe only createEditor in EDT
            val scope = project.service<LeanProjectService>().scope
            // The scope.launch here is mainly for the editor
            // ref: https://plugins.jetbrains.com/docs/intellij/coroutine-tips-and-tricks.html
            // TODO minimize the invoke later range
            scope.launch(Dispatchers.EDT) {
                infoViewWindow.updateEditorMouseMotionListener(
                    infoviewRender, file, position, // TODO this should add some UT for the rendering
                    interactiveGoals, interactiveTermGoal, interactiveDiagnostics, allMessage
                )
            }
        }

    }

    init {
        thisLogger().info("create infoview window using swing")
    }

    /**
     * TODO this seems called twice
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        ApplicationManager.getApplication().invokeLater {
            // This should be run in EDT
            // TODO check if it's necessary to pull call in EDT
            //      or, is there better way to do it
            val leanInfoViewWindow = LeanInfoViewWindow(toolWindow)
            val content = ContentFactory.getInstance().createContent(leanInfoViewWindow, null, false)
            toolWindow.contentManager.addContent(content)
            val leanInfoviewService = project.service<LeanInfoviewService>()
            leanInfoviewService.actionToolbar = configureToolbar(project, toolWindow)
        }
    }

    override fun shouldBeAvailable(project: Project) = true

    fun configureToolbar(project: Project, toolWindow: ToolWindow): ActionToolbar {
        val actions = DefaultActionGroup()
        val manager = ActionManager.getInstance()
        // actions.add(manager.getAction("ToggleAutomaticallyRefreshInternalInfoview"))
        actions.add(manager.getAction("RestartInternalInfoview"))
        actions.add(manager.getAction("RestartCurrentLeanFile"))
        actions.add(manager.getAction("RestartLeanLsp"))
        // actions.add(manager.getAction("IncreaseZoomLevelForLeanInfoView"))
        // actions.add(manager.getAction("DecreaseZoomLevelForLeanInfoView"))
        // actions.add(manager.getAction("ResetZoomLevelForLeanInfoView"))
        actions.add(manager.getAction("ToggleLeanInfoviewToolbarVisibility"))
        actions.add(manager.getAction("ToggleInternalInfoviewSoftWrap"))

        // TODO what is place for?
        val tb = manager.createActionToolbar("Lean Infoview", actions, true)

        tb.targetComponent = toolWindow.component
        tb.component.border = JBUI.Borders.merge(
            tb.component.border,
            JBUI.Borders.customLine(OnePixelDivider.BACKGROUND, 0, 0, 0, 1),
            true
        )
        toolWindow.component.add(tb.component, BorderLayout.NORTH)
        return tb
    }

}
