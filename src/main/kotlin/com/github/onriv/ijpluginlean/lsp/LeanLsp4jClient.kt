package com.github.onriv.ijpluginlean.lsp

import com.github.onriv.ijpluginlean.actions.BuildWindowManager
import com.github.onriv.ijpluginlean.lsp.data.FileProgressProcessingInfo
import com.github.onriv.ijpluginlean.project.FileProgress
import com.github.onriv.ijpluginlean.util.Constants
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification

/**
 * class for handling lsp notification
 */
class LeanLsp4jClient(project: Project) : LanguageClientImpl(project) {

    /**
     * TODO... should not run this with back program task indicator...
     *         if the lean file in .lake update, it's huge tasks
     */
    @JsonNotification(Constants.FILE_PROGRESS)
    fun leanFileProgress(params: FileProgressProcessingInfo) {
        FileProgress.run(project, params)
        BuildWindowManager.getInstance(project).fileProcess(params)
    }

}