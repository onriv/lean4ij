package lean4ij.lsp

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import lean4ij.util.Constants
import com.google.gson.reflect.TypeToken
import com.google.gson.JsonElement
import kotlinx.coroutines.future.await
import lean4ij.lsp.data.ContextInfo
import lean4ij.lsp.data.DefinitionTarget
import lean4ij.lsp.data.GetGoToLocationParams
import lean4ij.lsp.data.InfoPopup
import lean4ij.lsp.data.InfoViewContent
import lean4ij.lsp.data.InteractiveDiagnostics
import lean4ij.lsp.data.InteractiveDiagnosticsParams
import lean4ij.lsp.data.InteractiveGoal
import lean4ij.lsp.data.InteractiveGoals
import lean4ij.lsp.data.InteractiveGoalsParams
import lean4ij.lsp.data.InteractiveInfoParams
import lean4ij.lsp.data.InteractiveTermGoal
import lean4ij.lsp.data.InteractiveTermGoalParams
import lean4ij.lsp.data.LazyTraceChildrenToInteractiveParams
import lean4ij.lsp.data.MsgEmbed
import lean4ij.lsp.data.MsgEmbedExpr
import lean4ij.lsp.data.MsgEmbedGoal
import lean4ij.lsp.data.MsgEmbedTrace
import lean4ij.lsp.data.MsgUnsupported
import lean4ij.lsp.data.PlainGoal
import lean4ij.lsp.data.PlainGoalParams
import lean4ij.lsp.data.PlainTermGoal
import lean4ij.lsp.data.RpcCallParams
import lean4ij.lsp.data.PlainTermGoalParams
import lean4ij.lsp.data.RpcConnectParams
import lean4ij.lsp.data.RpcConnected
import lean4ij.lsp.data.RpcKeepAliveParams
import lean4ij.lsp.data.StrictOrLazy
import lean4ij.lsp.data.StrictOrLazyLazy
import lean4ij.lsp.data.StrictOrLazyStrict
import lean4ij.lsp.data.SubexprInfo
import lean4ij.lsp.data.TaggedText
import lean4ij.lsp.data.TaggedTextAppend
import lean4ij.lsp.data.TaggedTextTag
import lean4ij.lsp.data.TaggedTextText
import lean4ij.util.fromJson
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import java.lang.reflect.Type
import java.util.concurrent.CompletableFuture

/**
 * A wrapper around the [InternalLeanLanguageServer] that provides type-safe, suspending methods
 * for interacting with the Lean 4 Language Server.
 *
 * This class handles:
 * - Asynchronous communication with the LSP server.
 * - Serialization and deserialization of LSP messages using [Gson].
 * - Specific Lean 4 RPC method calls (e.g., for InfoView).
 *
 * Usage:
 * Typically accessed via [lean4ij.project.LeanFile] which handles session lifecycle and business logic.
 */
class LeanLanguageServer(val languageServer: InternalLeanLanguageServer) {

    /**
     * Retrieves the plain goal state at a specific position.
     *
     * @param params Parameters specifying the text document and position.
     * @return The [PlainGoal] containing the goal state, or null if not available.
     */
    suspend fun plainGoal(params: PlainGoalParams): PlainGoal? {
        return plainGoalAsync(params).await()
    }

    /**
     * Retrieves the plain term goal at a specific position.
     *
     * @param params Parameters specifying the text document and position.
     * @return The [PlainTermGoal] containing the term goal, or null.
     */
    suspend fun plainTermGoal(params: PlainTermGoalParams): PlainTermGoal? {
        return plainTermGoalAsync(params).await()
    }

    /**
     * Establishes an RPC session with the Lean server.
     *
     * @param params Connection parameters (usually containing the file URI).
     * @return [RpcConnected] containing the session ID.
     */
    suspend fun rpcConnect(params: RpcConnectParams): RpcConnected {
        return rpcConnectAsync(params).await()
    }

    /**
     * Executes a generic RPC call.
     *
     * @param params Parameters for the RPC call, including the method name and arguments.
     * @return The result as a [JsonElement], or null.
     */
    suspend fun rpcCall(params: RpcCallParams): JsonElement? {
        return rpcCallAsync(params).await()
    }

    /**
     * Retrieves interactive goals for the InfoView.
     *
     * @param params Parameters specifying the position and session.
     * @return [InteractiveGoals] structure for rendering.
     */
    suspend fun getInteractiveGoals(params: InteractiveGoalsParams): InteractiveGoals? {
        return getInteractiveGoalsAsync(params).await()
    }

    /**
     * Retrieves interactive term goals.
     *
     * @param params Parameters specifying the position and session.
     * @return [InteractiveTermGoal] structure.
     */
    suspend fun getInteractiveTermGoal(params: InteractiveTermGoalParams): InteractiveTermGoal? {
        return getInteractiveTermGoalAsync(params).await()
    }

    /**
     * Retrieves interactive diagnostics (errors, warnings) for the InfoView.
     *
     * @param params Parameters specifying the line range.
     * @return A list of [InteractiveDiagnostics], or null.
     */
    suspend fun getInteractiveDiagnostics(params: InteractiveDiagnosticsParams): List<InteractiveDiagnostics>? {
        return getInteractiveDiagnosticsAsync(params).await()
    }

    /**
     * Converts generic info to an interactive popup format.
     *
     * @param params Parameters containing the info to convert.
     * @return [InfoPopup] content.
     */
    suspend fun infoToInteractive(params: InteractiveInfoParams): InfoPopup {
        return infoToInteractiveAsync(params).await()
    }

    /**
     * Lazily resolves children of a trace message into interactive format.
     *
     * @param params Parameters specifying the trace node.
     * @return A list of [TaggedText] containing [MsgEmbed] messages.
     */
    suspend fun lazyTraceChildrenToInteractive(params: LazyTraceChildrenToInteractiveParams): List<TaggedText<MsgEmbed>>? {
        return lazyTraceChildrenToInteractiveAsync(params).await()
    }

    /**
     * Retrieves definition locations for "Go to Definition".
     *
     * @param params Parameters specifying the source location.
     * @return A list of [DefinitionTarget] locations.
     */
    suspend fun getGotoLocation(params: GetGoToLocationParams) : List<DefinitionTarget>? {
        return getGotoLocationAsync(params).await()
    }


    // -- Async Wrapper Methods --

    fun plainGoalAsync(params: PlainGoalParams): CompletableFuture<PlainGoal?> {
        return languageServer.plainGoal(params)
    }

    fun plainTermGoalAsync(params: PlainTermGoalParams): CompletableFuture<PlainTermGoal?> {
        return languageServer.plainTermGoal(params)
    }

    fun rpcConnectAsync(params: RpcConnectParams): CompletableFuture<RpcConnected> {
        return languageServer.rpcConnect(params)
    }

    fun rpcCallAsync(params: RpcCallParams): CompletableFuture<JsonElement?> {
        return languageServer.rpcCall(params)
    }

    fun getInteractiveGoalsAsync(params: InteractiveGoalsParams): CompletableFuture<InteractiveGoals?> {
        return languageServer.rpcCall(params).thenApply {
            gson.fromJson(it)
        }
    }

    fun getInteractiveTermGoalAsync(params: InteractiveTermGoalParams): CompletableFuture<InteractiveTermGoal?> {
        return languageServer.rpcCall(params).thenApply {
            gson.fromJson(it)
        }
    }

    fun getInteractiveDiagnosticsAsync(params: InteractiveDiagnosticsParams): CompletableFuture<List<InteractiveDiagnostics>?> {
        return languageServer.rpcCall(params).thenApply {
            gson.fromJson(it)
        }
    }

    /**
     * TODO: Investigate origin of [InteractiveInfoParams] and return type.
     */
    fun infoToInteractiveAsync(params: InteractiveInfoParams): CompletableFuture<InfoPopup> {
        return languageServer.rpcCall(params).thenApply {
            gson.fromJson(it)
        }
    }

    /**
     * RPC call for showing trace details in InfoView.
     */
    fun lazyTraceChildrenToInteractiveAsync(params: LazyTraceChildrenToInteractiveParams): CompletableFuture<List<TaggedText<MsgEmbed>>?> {
        return languageServer.rpcCall(params).thenApply {
            gson.fromJson(it)
        }
    }

    fun getGotoLocationAsync(params: GetGoToLocationParams) : CompletableFuture<List<DefinitionTarget>?> {
        return languageServer.rpcCall(params).thenApply {
            gson.fromJson(it)
        }
    }


    fun rpcKeepAlive(params: RpcKeepAliveParams) {
        return languageServer.rpcKeepAlive(params)
    }

    fun didClose(params: DidCloseTextDocumentParams) {
        return languageServer.didClose(params)
    }

    fun didOpen(params: DidOpenTextDocumentParams) {
        return languageServer.didOpen(params)
    }

    companion object {
        /**
         * Gson instance configured with custom deserializers for Lean 4 RPC types.
         *
         * Key configurations:
         * - Registers [TaggedText] adapters for [SubexprInfo] and [MsgEmbed].
         * - Custom deserializer for [MsgEmbed] to handle polymorphic types (expr, goal, trace).
         * - Custom deserializer for [RpcCallParams] to map method names to parameter types.
         * - Custom deserializer for [StrictOrLazy] to handle lazy loading structures.
         */
        val gson: Gson = GsonBuilder()
            .registerTaggedText<SubexprInfo>()
            .registerTaggedText<MsgEmbed>()
            .registerTypeAdapter(MsgEmbed::class.java, object : JsonDeserializer<MsgEmbed> {
                override fun deserialize(p0: JsonElement, p1: Type, p2: JsonDeserializationContext): MsgEmbed {
                    // TODO: Refactor similar deserializers to reduce duplication.
                    if (p0.isJsonObject && p0.asJsonObject.has("expr")) {
                        @Suppress("NAME_SHADOWING")
                        val p1 = p0.asJsonObject.getAsJsonObject("expr")
                        val f1: TaggedText<SubexprInfo> =
                            p2.deserialize(p1, object : TypeToken<TaggedText<SubexprInfo>>() {}.type)
                        return MsgEmbedExpr(f1)
                    }
                    if (p0.isJsonObject && p0.asJsonObject.has("goal")) {
                        @Suppress("NAME_SHADOWING")
                        val p1 = p0.asJsonObject.getAsJsonObject("goal")
                        val f1: InteractiveGoal = p2.deserialize(p1, InteractiveGoal::class.java)
                        return MsgEmbedGoal(f1)
                    }
                    if (p0.isJsonObject && p0.asJsonObject.has("trace")) {
                        @Suppress("NAME_SHADOWING")
                        val p1 = p0.asJsonObject.getAsJsonObject("trace")
                        val ret: MsgEmbedTrace = p2.deserialize(p1, MsgEmbedTrace::class.java)
                        return ret
                    }
                    if (p0.isJsonObject && p0.asJsonObject.has("widget")) {
                        return MsgUnsupported("Widget message cannot be supported for technical reason. Please use the jcef version infoview.")
                    }
                    throw IllegalStateException(p0.toString())
                }

            })
            .registerTypeAdapter(RpcCallParams::class.java, object : JsonDeserializer<RpcCallParams> {
                override fun deserialize(p0: JsonElement, p1: Type, p2: JsonDeserializationContext): RpcCallParams {
                    val method = p0.asJsonObject.getAsJsonPrimitive("method").asString
                    when (method) {
                        Constants.RPC_METHOD_INFO_TO_INTERACTIVE -> return p2.deserialize<InteractiveInfoParams>(
                            p0,
                            InteractiveInfoParams::class.java
                        )

                        Constants.RPC_METHOD_GET_INTERACTIVE_GOALS -> return p2.deserialize<InteractiveInfoParams>(
                            p0,
                            InteractiveGoalsParams::class.java
                        )
                    }
                    throw IllegalStateException("Unsupported RPC method: $method")
                }
            })
            .registerTypeAdapter(
                StrictOrLazy::class.java,
                object : JsonDeserializer<StrictOrLazy<List<TaggedText<MsgEmbed>>, ContextInfo>> {
                    override fun deserialize(
                        p0: JsonElement,
                        p1: Type,
                        p2: JsonDeserializationContext
                    ): StrictOrLazy<List<TaggedText<MsgEmbed>>, ContextInfo> {
                        if (p0.isJsonObject && p0.asJsonObject.has("strict")) {
                            val p0 = p0.asJsonObject.get("strict")
                            val children = p2.deserialize<List<TaggedText<MsgEmbed>>>(
                                p0,
                                object : TypeToken<List<TaggedText<MsgEmbed>>>() {}.type
                            )
                            return StrictOrLazyStrict(children)
                        }
                        if (p0.isJsonObject && p0.asJsonObject.has("lazy")) {
                            val p0 = p0.asJsonObject.get("lazy")
                            val children = p2.deserialize<ContextInfo>(p0, object : TypeToken<ContextInfo>() {}.type)
                            return StrictOrLazyLazy(children)
                        }
                        throw IllegalStateException("$p0 cannot be deserialized to StrictOrLazy<List<TaggedText<MsgEmbed>>, ContextInfo>")
                    }
                })
            .create()
    }
}

/**
 * Registers a [TaggedText] adapter for a specific [InfoViewContent] type [T].
 *
 * This extension function allows Gson to handle the recursive and polymorphic nature of [TaggedText].
 * It handles three cases:
 * 1. `tag`: A tagged text node (e.g., semantic info).
 * 2. `append`: A list of tagged text nodes.
 * 3. `text`: A raw string.
 *
 * @param T The type of content within the tag (e.g., [SubexprInfo], [MsgEmbed]).
 */
inline fun <reified T> GsonBuilder.registerTaggedText(): GsonBuilder where T : InfoViewContent {
    val type = object : TypeToken<TaggedText<T>>() {}.type
    this.registerTypeAdapter(type, object : JsonDeserializer<TaggedText<T>> {
        override fun deserialize(p0: JsonElement, p1: Type, p2: JsonDeserializationContext): TaggedText<T> {
            if (p0.isJsonObject && p0.asJsonObject.has("tag")) {
                @Suppress("NAME_SHADOWING")
                val p1 = p0.asJsonObject.getAsJsonArray("tag")
                val f0: T = p2.deserialize(p1.get(0), T::class.java)
                val f1: TaggedText<T> = p2.deserialize(p1.get(1), type)
                return TaggedTextTag(f0, f1)
            }
            if (p0.isJsonObject && p0.asJsonObject.has("append")) {
                @Suppress("NAME_SHADOWING")
                val p1 = p0.asJsonObject.getAsJsonArray("append")
                val r: MutableList<TaggedText<T>> = ArrayList()
                for (e in p1) {
                    r.add(p2.deserialize(e, type))
                }
                return TaggedTextAppend(r)
            }
            if (p0.isJsonObject && p0.asJsonObject.has("text")) {
                @Suppress("NAME_SHADOWING")
                val p1 = p0.asJsonObject.getAsJsonPrimitive("text").asString
                return TaggedTextText(p1)
            }
            throw IllegalStateException(p0.toString())
        }
    })
    // TODO not sure why but it requires for deserializing List<TaggedText<MsgEmbed>>
    val listType = object : TypeToken<List<TaggedText<T>>>() {}.type
    this.registerTypeAdapter(listType, object : JsonDeserializer<List<TaggedText<T>>> {
        override fun deserialize(p0: JsonElement, p1: Type, p2: JsonDeserializationContext): List<TaggedText<T>> {
            return p2.deserialize<List<JsonElement>>(p0, object : TypeToken<List<JsonElement>>() {}.type)
                .map { p2.deserialize(it, type) }
        }
    })
    return this
}
