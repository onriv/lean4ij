package com.github.onriv.ijpluginlean.lsp

import com.github.onriv.ijpluginlean.lsp.data.*
import com.google.gson.Gson
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.SyncViewManager
import com.intellij.build.events.impl.*
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException


class LeanLspServerManager (val project: Project, val languageServer: LeanLanguageServer) {

    companion object {
        private val projects = ConcurrentHashMap<Project, LeanLspServerManager>()
        fun getInstance(project: Project): LeanLspServerManager {
            return projects.computeIfAbsent(project) { k ->
                val servers = LanguageServiceAccessor.getInstance(project)
                    .getActiveLanguageServers{true}
                val server1 = servers.get(0) as LeanLanguageServer
                LeanLspServerManager(project, server1)
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
            // TODO in fact jetebrain's lsp impl has this
            // return "file:///"+url.substring(7,8).lowercase() +url.substring(8).replaceFirst(":", "%3A")
            // lsp4ij's way
            return "file:///"+url.substring(7,8) +url.substring(8)
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

    init {
        rpcKeepAlive()
    }

    private val sessions = ConcurrentHashMap<String, String>()

    // TODO maybe async way
    fun plainGoal(file: VirtualFile, caret: Caret) : List<String> {
        val textDocument = TextDocumentIdentifier(tryFixWinUrl(file.url))
        // val position = Position(line=line!!, character = column!!)
        val logicalPosition = caret.logicalPosition
        val position = Position(line=logicalPosition.line, character = logicalPosition.column)
        val resp = languageServer.plainGoal(
            PlainGoalParams( textDocument = textDocument, position = position )
        ).get()
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
        val resp = languageServer.plainTermGoal(
            PlainTermGoalParams( textDocument = textDocument, position = position )
        ).get()
        // TODO handle this null more seriously  and show it in the ui
        if (resp == null) {
            return ""
        }
        return resp.goal;
    }

    fun getInteractiveGoals(file: VirtualFile, caret: Caret, retry: Int = 0): Any? {
        val sessionId = getSession(file.toString(), retry > 1)
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
        try {
            // TODO async!
            val resp = languageServer.rpcCall(rpcParams).get()
            if (resp == null && retry < 2) {
                return getInteractiveGoals(file, caret, retry + 1)
            }
            return resp
        } catch (e: ExecutionException) {
            if (e.cause!!.message!!.contains("Outdated RPC session") && retry < 2) {
                return getInteractiveGoals(file, caret, retry + 1)
            }
            throw e;
        }
    }

    fun infoToInteractive(file: VirtualFile, caret: Caret, params: ContextInfo, retry: Int = 0): Any {
        // TODO DRY
        val sessionId = getSession(file.toString(), retry > 1)
        val textDocument = TextDocumentIdentifier(tryFixWinUrl(file.url))
        val logicalPosition = caret.logicalPosition
        val position = Position(line = logicalPosition.line, character = logicalPosition.column)
        val rpcParams = InteractiveInfoParams(
            sessionId = sessionId,
            method = "Lean.Widget.InteractiveDiagnostics.infoToInteractive",
            params = params,
            textDocument = textDocument,
            position = position
        )
        try {
            /**
             * rg.eclipse.lsp4j.jsonrpc.ResponseErrorException: Cannot decode params in RPC call 'Lean.Widget.InteractiveDiagnostics.infoToInteractive({"p":"6"})'
             * RPC reference '6' is not valid
             * java.util.concurrent.ExecutionException: org.eclipse.lsp4j.jsonrpc.ResponseErrorException: Cannot decode params in RPC call 'Lean.Widget.InteractiveDiagnostics.infoToInteractive({"p":"6"})'
             */
            val resp = languageServer.rpcCall(rpcParams).get()
            if (resp == null && retry < 2) {
                return infoToInteractive(file, caret, params, retry + 1)
            }
            return resp!!
        } catch (e: ExecutionException) {
            if (e.cause is ResponseErrorException && retry < 2) {
                return infoToInteractive(file, caret, params,retry + 1)
            }
            if (e.cause!!.message!!.contains("Outdated RPC session") && retry < 2) {
                return infoToInteractive(file, caret, params,retry + 1)
            }
            throw e;
        }
    }

    fun rpcCallRaw(any : Any, retry : Int = 0) : Any? {
        // TODO DRY
        // TODO better way, more type info
        // TODO in fact lsp4j 0.23 can take duplicate lsp request method, but I dont know why lsp4ij tae 0.21.1
        val any1 = any as java.util.Map<Any, Any>
        val file = (any["textDocument"] as java.util.Map<Any, Any>)["uri"]
        val sessionId = getSession(file.toString(), retry > 1)
        any1.put("sessionId", sessionId)
        try {
            return languageServer.rpcCall(any1).get()
        } catch (e: ExecutionException) {
            if (e.cause is ResponseErrorException && retry < 2) {
                return rpcCallRaw(any,retry + 1)
            }
            if (e.cause!!.message!!.contains("Outdated RPC session") && retry < 2) {
                return rpcCallRaw(any,retry + 1)
            }
            throw e;
        }
    }

    fun getSession(uri: String, force: Boolean = false): String {
        if (force) {
            sessions.remove(tryFixWinUrl(uri))
        }
        return sessions.computeIfAbsent(tryFixWinUrl(uri)) {connectRpc(it)}
    }

    private fun connectRpc(file : String) : String {
        val resp = languageServer.rpcConnect(RpcConnectParams(
            file
        )).get()
        // TODO handle exception here
        return resp!!.sessionId
    }

    /**
     * TODO do it in kotlin's coroutine!
     */
    fun rpcKeepAlive() {
        object : Thread() {
            override fun run() {
                while (true) {
                    for (entry in sessions.entries) {
                        languageServer.rpcKeepAlive(RpcKeepAliveParams(entry.key, entry.value))
                    }
                    sleep(9*1000)
                }
            }
        }.start()
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


       val syncProgress = SyncViewManager.createBuildProgress(project)

       // val descriptor = BuildProgressImpl(null, null, object : JComponent() {}, "curl --version")
       // syncProgress.start()

        // val syncId = ExternalSystemTaskId.create(systemId, ExternalSystemTaskType.RESOLVE_PROJECT, project)
        // val descriptor = DefaultBuildDescriptor(syncId, "curl --version", project.basePath!!, System.currentTimeMillis())
        // syncView.onEvent(descriptor, MessageEventImpl(syncId, MessageEvent.Kind.INFO, "test", "test11", "OOO"))


        val syncId = ExternalSystemTaskId.create(systemId, ExternalSystemTaskType.RESOLVE_PROJECT, project)
        val descriptor = DefaultBuildDescriptor(syncId, "curl", project.basePath!!, System.currentTimeMillis())
        var curlEvent = StartBuildEventImpl(descriptor, "--version")
        syncView.onEvent(descriptor, curlEvent)
        //
        syncView.onEvent(descriptor, OutputBuildEventImpl(syncId, "Test print", true))
        syncView.onEvent(descriptor, FinishBuildEventImpl(syncId, syncId, System.currentTimeMillis()+1000*50L, "DONE", SuccessResultImpl()))
         // syncView.onEvent(descriptor,kMessageEventImpl("curl", MessageEvent.Kind.INFO, "test", "test", "test"))


        // val descriptor = DefaultBuildDescriptor(syncId, "\$lean/fileProgress", project.basePath!!, System.currentTimeMillis())
        //     // .withRestartAction(action)
        //     // TODO what is this for?
        //     // The code here is copy from
        //     // intellij community plugins maven MavenSyncConsole.kt
        //     // .withRestartAction()
        //     // TODO with
        // // TODO weird, this seems not working
        // // syncView.onEvent(syncId, OutputBuildEventImpl(syncId, "Test print", true))
        // syncView.onEvent(descriptor, MessageEventImpl(syncId, MessageEvent.Kind.INFO, "test", "test11", "OOO"))
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