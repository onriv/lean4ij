package com.github.onriv.ijpluginlean.lsp

import com.github.onriv.ijpluginlean.lsp.data.PlainGoalParams
import com.github.onriv.ijpluginlean.lsp.data.PlainTermGoal
import com.github.onriv.ijpluginlean.lsp.data.PlainTermGoalParams
import com.github.onriv.ijpluginlean.lsp.data.Position
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture


class Lean4LanguageServer(val project: Project) : ProcessStreamConnectionProvider() {

    init {
        commands = Arrays.asList(
            // TODO configurable path for lake
            "~/.elan/toolchains/leanprover--lean4---v4.8.0-rc2/bin/lake".replaceFirst("~", System.getProperty("user.home"))
                .replace("/", File.separator),
            "serve", "--", project.basePath
        )
        workingDirectory = project.basePath
    }


}

class RpcConnectParams(
    val uri: String
)

/**
 * Lean declares sessionId as UInt32, it's kind of hard to work with in Java/Kotlin
 * like: 17710504432720554099 exit long
 * see: src/Lean/Data/Lsp/Extra.lean:124 to
 */
class RpcConnected(
    val sessionId: String
)

/**
/-- `$/lean/plainGoal` client<-server reply. -/
structure PlainGoal where
/-- The goals as pretty-printed Markdown, or something like "no goals" if accomplished. -/
rendered : String
/-- The pretty-printed goals, empty if all accomplished. -/
goals : Array String
deriving FromJson, ToJson
 */
class PlainGoal(
    val rendered: String,
    val goals: List<String>
)

/** there are two structures of range */
class Range(
    val start: Position,
    val end: Position,
)

class ProcessingInfo(
    val range: Range,
    val kind: Int
)

/**
structure LeanFileProgressProcessingInfo where
range : Range
kind : LeanFileProgressKind := LeanFileProgressKind.processing
deriving FromJson, ToJson
 */
class LeanFileProgressProcessingInfo(
    val textDocument: TextDocumentIdentifier,
    val processing: List<ProcessingInfo>
)

internal interface LeanLanguageServer : LanguageServer, TextDocumentService {

    @JsonRequest("\$/lean/plainGoal")
    fun plainGoal(params: PlainGoalParams): CompletableFuture<PlainGoal>

    @JsonRequest("\$/lean/plainTermGoal")
    fun plainTermGoal(params: PlainTermGoalParams): CompletableFuture<PlainTermGoal>

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
    fun rpcConnect(params: RpcConnectParams): CompletableFuture<RpcConnected>

    @JsonRequest("\$/lean/rpc/call")
    fun rpcCall(params: Any): CompletableFuture<Any?>

}
//
//
/**
 * TODO: The official lsp client from Jetbrains requires changing the initialize argument. Check if it's necessary here
 */
class Lean4LanguageServerFactory : LanguageServerFactory {
    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return Lean4LanguageServer(project)
    }

    // @NotNull  // If you need to provide client specific features
    // override fun createLanguageClient(@NotNull project: Project?): LanguageClientImpl {
    //     return MyLanguageClient(project)
    // }
    //
    override fun getServerInterface(): Class<out LanguageServer> {
        return LeanLanguageServer::class.java
    }

}