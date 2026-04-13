package lean4ij.lsp

import lean4ij.lsp.data.FileProgressProcessingInfo
import lean4ij.project.LeanProjectService
import lean4ij.util.Constants
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification


/**
 * Custom LSP client for handling Lean 4-specific server notifications.
 *
 * While the standard LSP protocol defines notifications like `textDocument/publishDiagnostics`,
 * Lean 4 introduces custom notifications such as `$/lean/fileProgress` that report compilation
 * progress per file. This client extends LSP4IJ's [LanguageClientImpl] to handle these
 * Lean-specific notifications.
 *
 * ## Handled Notifications
 * - **`$/lean/fileProgress`**: Receives [FileProgressProcessingInfo] from the server indicating
 *   which line ranges of a file are currently being processed by the Lean elaborator. This data
 *   drives the vertical progress bar in the editor gutter and the background progress indicator.
 *
 * @param project The IntelliJ project this client belongs to
 * @see LeanFile.updateFileProcessingInfo for how file progress is consumed downstream
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