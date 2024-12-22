package lean4ij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * TODO this is a very simple implementation
 *      add a tool window like maven and run anything support
 *      for lake
 * TODO this maybe unnecessary, the [lean4ij.run.LeanRunConfigurationType] should be enough
 *      although a seperated build window seems nice but it's IDE specific
 *      as said in https://plugins.jetbrains.com/docs/intellij/external-builder-api.html
 *      like checking PyCharm it has no Build option
 * ref:
 * - https://intellij-support.jetbrains.com/hc/en-us/community/posts/206759565-Help-With-Custom-Language-Plugin-External-BuildServer-Compiling
 * it seems intellij-erlang is the closest for this
 */
class LakeBuild : AnAction() {

    init {
        templatePresentation.icon = AllIcons.Actions.Compile
    }

    override fun actionPerformed(e: AnActionEvent) {

    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}