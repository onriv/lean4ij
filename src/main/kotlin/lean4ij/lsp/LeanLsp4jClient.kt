package lean4ij.lsp

import lean4ij.lsp.data.FileProgressProcessingInfo
import lean4ij.project.LeanProjectService
import lean4ij.util.Constants
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification


/**
 * class for handling lsp notification
 */
class LeanLsp4jClient(project: Project) : LanguageClientImpl(project) {

    private val leanProjectService : LeanProjectService = project.service()

    /**
     * TODO... should not run this with back program task indicator...
     *         if the lean file in .lake update, it's huge tasks
     */
    @JsonNotification(Constants.FILE_PROGRESS)
    fun leanFileProgress(params: FileProgressProcessingInfo) {
        leanProjectService.file(params.textDocument.uri).updateFileProcessingInfo(params)
        // TODO refactor this
        // BuildWindowMService.getInstance(project).fileProcess(params)
    }

}