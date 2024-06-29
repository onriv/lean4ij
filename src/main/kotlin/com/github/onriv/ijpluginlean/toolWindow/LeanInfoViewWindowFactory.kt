package com.github.onriv.ijpluginlean.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.github.onriv.ijpluginlean.services.MyProjectService
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea


class LeanInfoViewWindowFactory : ToolWindowFactory {

    companion object {

    }

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val leanInfoViewWindow = LeanInfoViewWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(leanInfoViewWindow, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class LeanInfoViewWindow(toolWindow: ToolWindow) : SimpleToolWindowPanel(true) {

        private val service = toolWindow.project.service<MyProjectService>()
        private val goals = JBTextArea("No goals")

        init {
            setContent( JBPanel<JBPanel<*>>().apply {
                add(goals)
            })
        }

        fun updateGoal(goal: String) {
            goals.text = goal
//            goals.revalidate();
//            goals.updateUI();
        }
    }
}
