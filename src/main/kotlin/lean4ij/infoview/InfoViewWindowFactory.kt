package lean4ij.infoview

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
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
import lean4ij.lsp.data.InteractiveDiagnostics
import lean4ij.lsp.data.InteractiveGoals
import lean4ij.lsp.data.InteractiveTermGoal
import lean4ij.lsp.data.Position
import lean4ij.project.LeanProjectService
import lean4ij.setting.Lean4Settings
import java.awt.BorderLayout

/**
 * TODO do not show this if indexing, the doc seems saying it's an option for it
 *      maybe dumb-aware or something
 */
class InfoViewWindowFactory : ToolWindowFactory {

    companion object {

        val settings = service<Lean4Settings>()

        /**
         * The id is from plugin.xml
         */
        fun getLeanInfoview(project: Project): LeanInfoViewWindow? {
            val contents = ToolWindowManager.getInstance(project)
                .getToolWindow("LeanInfoViewWindow")
                ?.contentManager?.contents
            // If the project just startup and teh tool window never opened before
            // it may be null
            if (contents == null) {
                thisLogger().debug("Cannot get lean info tool window contents")
                return null
            }
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
                if (settings.showExpectedTypeInInternalInfoview) {
                    interactiveTermGoal?.toInfoObjectModel()?.let {
                        // if it has interactive goals, then there should be some line break
                        // for interactive term goal, i.e., expected type
                        if (size > 0) {
                            br()
                        }
                        add(it)
                        size++
                    }
                }
                // TODO Although the option controls the UI visibility of the messages section,
                //      it does not disable the underlying logic responsible for generating this data.
                //      Consequently, certain LSP requests and backend operations related to messages
                //      are still executed even when the option is disabled, leading to redundant processing.
                if (settings.showMessagesInInternalInfoview && interactiveDiagnostics?.isNotEmpty() == true) {
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
                            }
                            if (idx != interactiveDiagnostics.lastIndex) {
                                br()
                            }
                        }
                    }
                }
                if (size == 0) {
                    +"No info found."
                }
            }
            // TODO Although the option controls the UI visibility of the allMessages section,
            //      it does not disable the underlying logic responsible for generating this data.
            //      Consequently, certain LSP requests and backend operations related to allMessages
            //      are still executed even when the option is disabled, leading to redundant processing.
            if (settings.showAllMessagesInInternalInfoview && !allMessage.isNullOrEmpty()) {
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
                infoViewWindow.updateEditorMouseMotionListener(context)
            }
        }

        /**
         * TODO should this be a setting?
         */
        var expandAllMessage: Boolean = false

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
        actions.add(manager.getAction("FindInInternalInfoview"))

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
