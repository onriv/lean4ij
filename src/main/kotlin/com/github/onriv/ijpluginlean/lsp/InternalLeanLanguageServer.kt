package com.github.onriv.ijpluginlean.lsp

import com.github.onriv.ijpluginlean.lsp.data.*
import com.github.onriv.ijpluginlean.util.Constants
import com.google.gson.JsonElement
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import java.util.concurrent.CompletableFuture

/**
 * defining interface for language server
 */
interface InternalLeanLanguageServer : LanguageServer, TextDocumentService {

    @JsonRequest(Constants.LEAN_PLAIN_GOAL)
    fun plainGoal(params: PlainGoalParams): CompletableFuture<PlainGoal>

    @JsonRequest(Constants.LEAN_PLAIN_TERM_GOAL)
    fun plainTermGoal(params: PlainTermGoalParams): CompletableFuture<PlainTermGoal>

    /**
     * `$/lean/rpc/connect` client->server request.
     *
     * Starts an RPC session at the given file's worker, replying with the new session ID.
     * Multiple sessions may be started and operating concurrently.
     *
     * A session may be destroyed by the server at any time (e.g. due to a crash), in which case further
     * RPC requests for that session will reply with `RpcNeedsReconnect` errors. The client should discard
     * references held from that session and `connect` again.
     *
     * ref: [src/Lean/Data/Lsp/Extra.lean#L109](https://github.com/leanprover/lean4/blob/23e49eb519a45496a9740aeb311bf633a459a61e/src/Lean/Data/Lsp/Extra.lean#L109)
     */
    @JsonRequest(Constants.LEAN_RPC_CONNECT)
    fun rpcConnect(params: RpcConnectParams): CompletableFuture<RpcConnected>

    /**
     * Here `$/lean/rpc/call` request can take a param `method`, if using lsp4j version 0.23, then the json request
     * can be duplicate, but not lsp4j 0.21. Hence, we still define only one json request, and in [LeanLspServerManager] we dispatch this request
     * for different method
     * see: [src/Lean/Server/Rpc/RequestHandling.lean#L36](https://github.com/leanprover/lean4/blob/23e49eb519a45496a9740aeb311bf633a459a61e/src/Lean/Server/Rpc/RequestHandling.lean#L36)
     */
    @JsonRequest(Constants.LEAN_RPC_CALL)
    fun rpcCall(params: RpcCallParams): CompletableFuture<JsonElement?>

    @JsonNotification(Constants.LEAN_RPC_KEEP_ALIVE)
    fun rpcKeepAlive(params: RpcKeepAliveParams)

}