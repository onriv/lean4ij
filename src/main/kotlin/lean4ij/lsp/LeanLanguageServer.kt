package lean4ij.lsp

import com.google.gson.*
import lean4ij.lsp.data.*
import lean4ij.util.Constants
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.future.await
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import java.lang.reflect.Type
import java.util.concurrent.CompletableFuture

/**
 * TODO Add gson util back?
 */
inline fun <reified T> fromJson(src: String): T {
    // val type = object : TypeToken<T>() {}.type
    return Gson().fromJson(src, T::class.java)
}

/**
 * This class declares the suspend like method for the language server, and handling serialization serialization
 * which making the method type safe, for usage see [lean4ij.project.LeanFile], where handles things like session
 * outdated and is more biz relevant.
 * TODO here we don't impl getWidgets. It's implemented with react and we can no do it with swing anyway...
 *      for the external infoview, it's bridged transparently and doesn't require an explicit declaration
 */
class LeanLanguageServer(val languageServer: InternalLeanLanguageServer) {

    suspend fun plainGoal (params: PlainGoalParams): PlainGoal? {
        return plainGoalAsync(params).await()
    }

    suspend fun plainTermGoal(params: PlainTermGoalParams): PlainTermGoal? {
        return plainTermGoalAsync(params).await()
    }

    suspend fun rpcConnect(params: RpcConnectParams): RpcConnected {
        return rpcConnectAsync(params).await()
    }

    suspend fun rpcCall(params: RpcCallParams): JsonElement? {
        return rpcCallAsync(params).await()
    }

    suspend fun getInteractiveGoals(params: InteractiveGoalsParams) : InteractiveGoals? {
        return getInteractiveGoalsAsync(params).await()
    }

    suspend fun getInteractiveTermGoal(params : InteractiveTermGoalParams) : InteractiveTermGoal? {
        return getInteractiveTermGoalAsync(params).await()
    }

    suspend fun getInteractiveDiagnostics(params : InteractiveDiagnosticsParams) : List<InteractiveDiagnostics>? {
        return getInteractiveDiagnosticsAsync(params).await()
    }

    suspend fun infoToInteractive(params: InteractiveInfoParams) : InfoPopup {
        return infoToInteractiveAsync(params).await()
    }

    fun plainGoalAsync (params: PlainGoalParams): CompletableFuture<PlainGoal?> {
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

    fun getInteractiveGoalsAsync(params: InteractiveGoalsParams) : CompletableFuture<InteractiveGoals?> {
        return languageServer.rpcCall(params).thenApply {
            gson.fromJson(it, InteractiveGoals::class.java)
        }
    }

    fun getInteractiveTermGoalAsync(params: InteractiveTermGoalParams) : CompletableFuture<InteractiveTermGoal?> {
        return languageServer.rpcCall(params).thenApply {
            gson.fromJson(it, InteractiveTermGoal::class.java)
        }
    }

    fun getInteractiveDiagnosticsAsync(params : InteractiveDiagnosticsParams): CompletableFuture<List<InteractiveDiagnostics>?> {
        return languageServer.rpcCall(params).thenApply {
            gson.fromJson(it, object : TypeToken<List<InteractiveDiagnostics>>(){}.type)
        }
    }

    /**
     * TODO weird, where is this params [InteractiveInfoParams] and return result [CodeWithInfos]
     *      from?   it seems incorrect
     */
    fun infoToInteractiveAsync(params: InteractiveInfoParams) : CompletableFuture<InfoPopup> {
        return languageServer.rpcCall(params).thenApply {
            gson.fromJson(it, InfoPopup::class.java)
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
         * TODO here it can be some refactor to DRY
         */
        val gson: Gson = GsonBuilder()
            .registerTaggedText<SubexprInfo>()
            .registerTaggedText<MsgEmbed>()
            .registerTypeAdapter(MsgEmbed::class.java, object :JsonDeserializer<MsgEmbed> {
                override fun deserialize(p0: JsonElement, p1: Type, p2: JsonDeserializationContext): MsgEmbed {
                    // TODO these and all around deserializer is very similar, maybe refactor them
                    if (p0.isJsonObject && p0.asJsonObject.has("expr")) {
                        @Suppress("NAME_SHADOWING")
                        val p1 = p0.asJsonObject.getAsJsonObject("expr")
                        val f1: TaggedText<SubexprInfo> = p2.deserialize(p1, object : TypeToken<TaggedText<SubexprInfo>>() {}.type)
                        return MsgEmbedExpr(f1)
                    }
                    if (p0.isJsonObject && p0.asJsonObject.has("goal")) {
                        @Suppress("NAME_SHADOWING")
                        val p1 = p0.asJsonObject.getAsJsonObject("goal")
                        val f1: InteractiveGoal = p2.deserialize(p1, InteractiveGoal::class.java)
                        return MsgEmbedGoal(f1)
                    }
                    if (p0.isJsonObject && p0.asJsonObject.has("trace")) {
                        return MsgUnsupported("Trace message is not supported yet. Please the jcef version infoview.")
                    }
                    if (p0.isJsonObject && p0.asJsonObject.has("widget")) {
                        return MsgUnsupported("Widget message cannot be supported for technical reason. Please the jcef version infoview.")
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
            .create()
    }
}

/**
 * this is hinted by copilot
 * TODO does it already exists in some library? I think it must be
 * TODO add the log for it with the old scala log also for similar purpose
 *      and in fact the only crucial part it
 *      val type = object : TypeToken<TaggedText<T>>() {}.type
 *      all other is already support in Gson (kind of forgetting this)
 */
inline fun <reified T> GsonBuilder.registerTaggedText(): GsonBuilder where T: InfoViewContent  {
    val type = object : TypeToken<TaggedText<T>>() {}.type
    return this.registerTypeAdapter(type, object : JsonDeserializer<TaggedText<T>> {
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
}
