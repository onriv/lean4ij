package com.github.onriv.ijpluginlean.lsp

import com.github.onriv.ijpluginlean.lsp.data.PlainGoalParams
import com.github.onriv.ijpluginlean.lsp.data.Position
import com.github.onriv.ijpluginlean.lsp.data.RpcCallParams
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import org.eclipse.lsp4j.TextDocumentIdentifier
import java.util.concurrent.ConcurrentHashMap

class LeanLspServerManager (private val lspServer: LspServer) {

    companion object {
        private val projects = ConcurrentHashMap<Project, LeanLspServerManager>()
        fun getInstance(project: Project): LeanLspServerManager {
            return projects.computeIfAbsent(project) { k ->
                val lspServer = LspServerManager.getInstance(k)
                    .getServersForProvider(LeanLspServerSupportProvider::class.java).firstOrNull();
                LeanLspServerManager(lspServer!!)
            }
        }

    }

    private val sessions = ConcurrentHashMap<VirtualFile, String>()

    fun getPlainGoal(file: VirtualFile, caret: Caret) : List<String> {
        val textDocument = TextDocumentIdentifier(tryFixWinUrl(file.url))
        // val position = Position(line=line!!, character = column!!)
        val logicalPosition = caret.logicalPosition
        val position = Position(line=logicalPosition.line, character = logicalPosition.column)
        val resp = lspServer.sendRequestSync {(it as LeanLanguageServer).leanPlainGoal(
            PlainGoalParams( textDocument = textDocument, position = position )
        )}
        return resp!!.goals;
    }

    fun getInteractiveGoals(file: VirtualFile, caret: Caret): Any {
        val sessionId = sessions.computeIfAbsent(file) {connectRpc(it)}
        val textDocument = TextDocumentIdentifier(tryFixWinUrl(file.url))
        val logicalPosition = caret.logicalPosition
        val position = Position(line=logicalPosition.line, character = logicalPosition.column)
        val rpcParams = RpcCallParams(
            sessionId = sessionId,
            method = "Lean.Widget.getInteractiveGoals",
            params = PlainGoalParams(
                textDocument = textDocument,
                position = position
            ),
            textDocument = textDocument,
            position = position
        )
        // TODO according to lean's src code, here it's chance it failed
        //      and must reconnect
        val resp = lspServer.sendRequestSync { (it as LeanLanguageServer).rpcCall(rpcParams) }
        return resp!!
    }

    private fun connectRpc(file : VirtualFile) : String {
        val resp = lspServer.sendRequestSync { (it as LeanLanguageServer).rpcConnect(RpcConnectParams(
            tryFixWinUrl(file.url)
        )) }
        // TODO handle exception here
        return resp!!.sessionId
    }

    /**
     * See the document of [com.intellij.platform.lsp.api.LspServerDescriptor#getFileUri]
     * for the fix here:
     * The LSP spec [requires](https://microsoft.github.io/language-server-protocol/specification/#uri)
     * that all servers work fine with URIs in both formats: `file:///C:/foo` and `file:///c%3A/foo`.
     *
     * VS Code always sends a lowercased Windows drive letter, and always escapes colon
     * (see this [issue](https://github.com/microsoft/vscode-languageserver-node/issues/1280)
     * and the related [pull request](https://github.com/microsoft/language-server-protocol/pull/1786)).
     *
     * Some LSP servers support only the VS Code-friendly URI format (`file:///c%3A/foo`), so it's safer to use it by default.
     */
    private fun tryFixWinUrl(url: String) : String {
        if (!isWindows()) {
            return url
        }
        // this is for windows ...
        // TODO check it in linux/macos
        // lean lsp server is using lowercase disk name
        // TODO this is so ugly, make it better
        // TODO in fact
        return "file:///"+url.substring(7,8).lowercase() +url.substring(8).replaceFirst(":", "%3A")
    }

    private fun detectOperatingSystem(): String {
        val osName = System.getProperty("os.name").lowercase()

        return when {
            "windows" in osName -> "Windows"
            listOf("mac", "nix", "sunos", "solaris", "bsd").any { it in osName } -> "*nix"
            else -> "Other"
        }
    }

    private fun isWindows() : Boolean {
        return detectOperatingSystem() == "Windows";
    }

}