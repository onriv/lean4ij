package lean4ij.lsp

import lean4ij.lsp.data.*
import lean4ij.util.Constants
import com.google.gson.JsonElement
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import java.util.concurrent.CompletableFuture

/**
 * Raw LSP interface contract for the Lean 4 language server, extending the standard LSP protocol.
 *
 * This interface defines the Lean-specific JSON-RPC methods that go beyond the standard
 * [LanguageServer] and [TextDocumentService] protocols. LSP4IJ uses this interface to
 * generate the JSON-RPC proxy that communicates with the `lake serve` process.
 *
 * **Important**: This interface is not used directly by plugin code. Instead, [LeanLanguageServer]
 * wraps this interface and provides higher-level methods that handle RPC session management,
 * Gson deserialization, and async dispatch.
 *
 * ## Lean-Specific Methods
 * - **`$/lean/plainGoal`**: Returns the plain-text goal state at a cursor position.
 * - **`$/lean/plainTermGoal`**: Returns the expected type at a cursor position.
 * - **`$/lean/rpc/connect`**: Establishes an RPC session for a file, returning a session ID.
 * - **`$/lean/rpc/call`**: Generic RPC call endpoint that dispatches to various Lean widget
 *   methods (e.g., `getInteractiveGoals`, `getInteractiveDiagnostics`) based on the `method`
 *   field in params.
 * - **`$/lean/rpc/keepAlive`**: Heartbeat to prevent RPC session timeout.
 *
 * @see LeanLanguageServer for the high-level wrapper that dispatches typed RPC calls
 * @see <a href="https://github.com/leanprover/lean4/blob/master/src/Lean/Data/Lsp/Extra.lean">
 *   Lean LSP Extra definitions</a>
 */
interface InternalLeanLanguageServer : LanguageServer, TextDocumentService {

    @JsonRequest(Constants.LEAN_PLAIN_GOAL)
    fun plainGoal(params: PlainGoalParams): CompletableFuture<PlainGoal?>

    @JsonRequest(Constants.LEAN_PLAIN_TERM_GOAL)
    fun plainTermGoal(params: PlainTermGoalParams): CompletableFuture<PlainTermGoal?>

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
     * can be duplicate, but not lsp4j 0.21. Hence, we still define only one json request, and in [LeanLanguageServer] we dispatch this request
     * for different method
     * see: [src/Lean/Server/Rpc/RequestHandling.lean#L36](https://github.com/leanprover/lean4/blob/23e49eb519a45496a9740aeb311bf633a459a61e/src/Lean/Server/Rpc/RequestHandling.lean#L36)
     */
    @JsonRequest(Constants.LEAN_RPC_CALL)
    fun rpcCall(params: RpcCallParams): CompletableFuture<JsonElement?>

    @JsonNotification(Constants.LEAN_RPC_KEEP_ALIVE)
    fun rpcKeepAlive(params: RpcKeepAliveParams)

}