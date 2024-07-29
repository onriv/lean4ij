package com.github.onriv.ijpluginlean.project

import com.github.onriv.ijpluginlean.infoview.external.ExternalInfoViewService
import com.github.onriv.ijpluginlean.project.listeners.LeanFileCaretListener
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * see: [defining-project-level-listeners](https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html#defining-project-level-listeners)
 * this is copied from [ConnectDocumentToLanguageServerSetupParticipant](https://github.com/redhat-developer/lsp4ij/blob/cb04e064f93ec8c2bb22b216e54b6a7fb1c75496/src/main/java/com/redhat/devtools/lsp4ij/ConnectDocumentToLanguageServerSetupParticipant.java#L29)
 * no more from the above, now it implements [ProjectActivity]
 * ref: https://plugins.jetbrains.com/docs/intellij/plugin-components.html#project-and-application-close
 * and https://github.com/JetBrains/intellij-sdk-code-samples/blob/main/max_opened_projects/src/main/kotlin/org/intellij/sdk/maxOpenProjects/ProjectOpenStartupActivity.kt
 */
class LeanProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        project.service<LeanProjectService>()
        project.service<LeanFileCaretListener>()
        project.service<ExternalInfoViewService>()
    }

}