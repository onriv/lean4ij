package com.github.onriv.ijpluginlean.lsp

import com.github.onriv.ijpluginlean.lsp.data.RpcCallParams
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.*
import com.jetbrains.rd.generator.nova.PredefinedType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.CompletableFuture

internal class LeanLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        if (file.extension == "lean") {
            serverStarter.ensureServerStarted(LeanLspServerDescriptor(project))

        }
    }
}

private class LeanLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Lean") {
    override fun isSupportedFile(file: VirtualFile) = file.extension == "lean"
    override fun createCommandLine() = GeneralCommandLine(
        "~/.elan/toolchains/leanprover--lean4---v4.8.0-rc1/bin/lean".replaceFirst("~", System.getProperty("user.home")),
        "--server", project.basePath
    ).withEnvironment("LEAN_SERVER_LOG_DIR", System.getProperty("user.home"))

    override fun createInitializeParams(): InitializeParams {
        val ret = super.createInitializeParams()
        // see: https://leanprover.zulipchat.com/#narrow/stream/113488-general/topic/.E2.9C.94.20Lean.20LSP.20extensions/near/444888746
        // don't know what is used for though
        ret.capabilities.workspace.applyEdit = true
        return ret
    }

    override fun createLsp4jClient(handler: LspServerNotificationsHandler): Lsp4jClient {
        return super.createLsp4jClient(handler)
    }

    /**
     * copied from https://github.com/tomblachut/svelte-intellij/blob/master/src/main/java/dev/blachut/svelte/lang/service/SvelteLspServerSupportProvider.kt
     * and
     * https://github.com/huggingface/llm-intellij/blob/main/src/main/kotlin/co/huggingface/llmintellij/lsp/LlmLsLspServerDescriptor.kt#L31
     * it differs with the document:
     * https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html#customization
     */
    override val lsp4jServerClass: Class<out LanguageServer> = LeanLanguageServer::class.java
}

class RpcConnectParams(
    val uri: String
)

class RpcConnectResp(
    val sessionId: Long
)




internal interface LeanLanguageServer : LanguageServer {
    /**
     * /-- `$/lean/rpc/connect` client->server request.
     *
     * Starts an RPC session at the given file's worker, replying with the new session ID.
     * Multiple sessions may be started and operating concurrently.
     *
     * A session may be destroyed by the server at any time (e.g. due to a crash), in which case further
     * RPC requests for that session will reply with `RpcNeedsReconnect` errors. The client should discard
     * references held from that session and `connect` again. -/
     * ref: https://github.com/leanprover/lean4/blob/6b93f05cd172640253ad1ed27935167e5a3af981/src/Lean/Data/Lsp/Extra.lean
     */
    @JsonRequest("\$/lean/rpc/connect")
    fun rpcConnect(params: RpcConnectParams): CompletableFuture<RpcConnectResp>;

    @JsonRequest("\$/lean/rpc/call")
    fun rpcCall(params: RpcCallParams): CompletableFuture<Any>;
}