package lean4ij.infoview.external

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.OnePixelDivider
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout

class JcefInfoviewTooWindowFactory : ToolWindowFactory {

    companion object {
        /**
         * The id is defined in plugin.xml
         */
        fun getToolWindow(project: Project): ToolWindow? =
            ToolWindowManager.getInstance(project).getToolWindow("LeanInfoviewJcef")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val jcefInfoview = SimpleToolWindowPanel(true)
        val jcefService = project.service<JcefInfoviewService>()
        val browser = jcefService.browser
        if (browser != null) {
            // There is a concept named speed search built in list or tree etc.
            // see: https://plugins.jetbrains.com/docs/intellij/search-field.html#icons
            // and maybe things like com.intellij.ui.speedSearch.SpeedSearchSupply
            // and com.intellij.openapi.wm.impl.welcomeScreen.recentProjects.RecentProjectFilteringTree
            browser.component.add(jcefService.searchTextField, BorderLayout.NORTH)
            jcefInfoview.add(browser.component)
        } else {
            jcefInfoview.add(panel {
                row(jcefService.errMsg) {}
            })
        }
        val content = ContentFactory.getInstance().createContent(jcefInfoview, null, false)
        toolWindow.contentManager.addContent(content)
        jcefService.actionToolbar = configureToolbar(project, toolWindow)
    }

    fun configureToolbar(project: Project, toolWindow: ToolWindow): ActionToolbar {
        val actions = DefaultActionGroup()
        val manager = ActionManager.getInstance()
        actions.add(manager.getAction("RestartJcefInfoview"))
        actions.add(manager.getAction("RestartCurrentLeanFile"))
        actions.add(manager.getAction("RestartLeanLsp"))
        actions.add(manager.getAction("OpenExternalInfoviewInBrowser"))
        actions.add(manager.getAction("IncreaseZoomLevelForLeanInfoViewJcef"))
        actions.add(manager.getAction("DecreaseZoomLevelForLeanInfoViewJcef"))
        actions.add(manager.getAction("ResetZoomLevelForLeanInfoViewJcef"))
        actions.add(manager.getAction("ToggleLeanInfoviewJcefToolbarVisibility"))
        actions.add(manager.getAction("FindInExternalInfoview"))

        // TODO what is place for?
        val tb = manager.createActionToolbar("Lean Jcef Infoview", actions, true)

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
