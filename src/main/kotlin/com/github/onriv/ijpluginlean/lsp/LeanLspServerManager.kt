package com.github.onriv.ijpluginlean.lsp

import com.github.onriv.ijpluginlean.lsp.data.*
import com.intellij.build.BuildContentDescriptor
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.SyncViewManager
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.OutputBuildEvent
import com.intellij.build.events.StartBuildEvent
import com.intellij.build.events.impl.MessageEventImpl
import com.intellij.build.events.impl.OutputBuildEventImpl
import com.intellij.build.events.impl.StartBuildEventImpl
import com.intellij.build.events.impl.StartEventImpl
import com.intellij.build.progress.BuildProgressDescriptor
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JComponent

class LeanLspServerManager (val project: Project, val lspServer: LspServer) {

    companion object {
        private val projects = ConcurrentHashMap<Project, LeanLspServerManager>()
        fun getInstance(project: Project): LeanLspServerManager {
            return projects.computeIfAbsent(project) { k ->
                val lspServer = LspServerManager.getInstance(k)
                    .getServersForProvider(LeanLspServerSupportProvider::class.java).firstOrNull();
                LeanLspServerManager(k, lspServer!!)
            }
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
         *
         * TODO check idempotent
         */
        fun tryFixWinUrl(url: String) : String {
            if (url.startsWith("file:///")) {
                return url
            }
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

    private val sessions = ConcurrentHashMap<String, String>()

    fun plainGoal(file: VirtualFile, caret: Caret) : List<String> {
        val textDocument = TextDocumentIdentifier(tryFixWinUrl(file.url))
        // val position = Position(line=line!!, character = column!!)
        val logicalPosition = caret.logicalPosition
        val position = Position(line=logicalPosition.line, character = logicalPosition.column)
        val resp = lspServer.sendRequestSync {(it as LeanLanguageServer).plainGoal(
            PlainGoalParams( textDocument = textDocument, position = position )
        )}
        // TODO handle this null more seriously  and show it in the ui
        if (resp == null) {
            return ArrayList()
        }
        return resp.goals;
    }

    fun plainTermGoal(file: VirtualFile, caret: Caret) : String {
        val textDocument = TextDocumentIdentifier(tryFixWinUrl(file.url))
        // val position = Position(line=line!!, character = column!!)
        val logicalPosition = caret.logicalPosition
        val position = Position(line=logicalPosition.line, character = logicalPosition.column)
        val resp = lspServer.sendRequestSync {(it as LeanLanguageServer).plainTermGoal(
            PlainTermGoalParams( textDocument = textDocument, position = position )
        )}
        // TODO handle this null more seriously  and show it in the ui
        if (resp == null) {
            return ""
        }
        return resp.goal;
    }

    fun getInteractiveGoals(file: VirtualFile, caret: Caret, retry: Int = 0): Any {
        val sessionId = getSession(file.toString())
        val textDocument = TextDocumentIdentifier(tryFixWinUrl(file.url))
        val logicalPosition = caret.logicalPosition
        val position = Position(line=logicalPosition.line, character = logicalPosition.column)
        val rpcParams = InteractiveGoalsParams(
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
        try {
            val resp = lspServer.sendRequestSync { (it as LeanLanguageServer).rpcCall(rpcParams) }
            return resp!!
        } catch (e: ResponseErrorException) {
            if (e.message!!.contains("Outdated RPC session") && retry < 2) {
                return getInteractiveGoals(file, caret, retry + 1)
            }
            throw e;
        }
    }

    fun getSession(uri: String): String {
        return sessions.computeIfAbsent(tryFixWinUrl(uri)) {connectRpc(it)}
    }

    private fun connectRpc(file : String) : String {
        val resp = lspServer.sendRequestSync { (it as LeanLanguageServer).rpcConnect(RpcConnectParams(
            file
        )) }
        // TODO handle exception here
        return resp!!.sessionId
    }

    private val systemId = ProjectSystemId("LEAN4")
    private val syncView = project.service<SyncViewManager>()

    private var importStarted = false

    @Synchronized
    fun startImport() {
        if (importStarted) {
            return
        }
        importStarted = true
        val action : AnAction = object : AnAction("Sync") {
            override fun actionPerformed(e: AnActionEvent) {
                TODO("Not yet implemented")
            }

            override fun getActionUpdateThread() = ActionUpdateThread.BGT
        }

        val syncId = ExternalSystemTaskId.create(systemId, ExternalSystemTaskType.RESOLVE_PROJECT, project)
        val descriptor = DefaultBuildDescriptor(syncId, "\$lean/fileProgress", project.basePath!!, System.currentTimeMillis())
            // .withRestartAction(action)
            // TODO what is this for?
            // The code here is copy from
            // intellij community plugins maven MavenSyncConsole.kt
            // .withRestartAction()
            // TODO with
        syncView.onEvent(descriptor, StartBuildEventImpl(descriptor, project.name))
        // TODO weird, this seems not working
        // syncView.onEvent(syncId, OutputBuildEventImpl(syncId, "Test print", true))
        syncView.onEvent(descriptor, MessageEventImpl(syncId, MessageEvent.Kind.INFO, "test", "test11", "OOO"))
    }

    /**
     * copy from
     * https://github.com/intellij-rust/intellij-rust/blob/c6657c02bb62075bf7b7ceb84d000f93dda34dc1/src/main/kotlin/org/rust/cargo/project/model/impl/CargoSyncTask.kt#L13
     */
//    fun startImport1() {
//        thisLogger().debug("Start import ${project.name}")
//        val syncProgress = SyncViewManager.createBuildProgress(project)
//
//        val descriptor = BuildContentDescriptor(null, null, object : JComponent() {}, "File Progress")
//        val syncId = ExternalSystemTaskId.create(systemId, ExternalSystemTaskType.RESOLVE_PROJECT, project)
//        syncProgress.start(descriptor)
//    }
}