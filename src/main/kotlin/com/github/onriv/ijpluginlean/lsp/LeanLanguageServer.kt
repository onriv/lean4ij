package com.github.onriv.ijpluginlean.lsp

import com.github.onriv.ijpluginlean.lsp.data.*
import com.github.onriv.ijpluginlean.util.Constants
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import kotlinx.coroutines.future.await
import java.lang.reflect.Type
import java.util.concurrent.CompletableFuture

class LeanLanguageServer(private val languageServer: InternalLeanLanguageServer) {

    suspend fun plainGoal (params: PlainGoalParams): PlainGoal {
        return plainGoalAsync(params).await()
    }

    suspend fun plainTermGoal(params: PlainTermGoalParams): PlainTermGoal {
        return plainTermGoalAsync(params).await()
    }

    suspend fun rpcConnect(params: RpcConnectParams): RpcConnected {
        return rpcConnectAsync(params).await()
    }

    suspend fun rpcCall(params: RpcCallParams): JsonElement? {
        return rpcCallAsync(params).await()
    }

    suspend fun getInteractiveGoals(params: InteractiveGoalsParams) : InteractiveGoals {
        return getInteractiveGoalsAsync(params).await()
    }

    suspend fun infoToInteractive(params: InteractiveInfoParams) : CodeWithInfos {
        return infoToInteractiveAsync(params).await()
    }

    fun plainGoalAsync (params: PlainGoalParams): CompletableFuture<PlainGoal> {
        return languageServer.plainGoal(params)
    }

    fun plainTermGoalAsync(params: PlainTermGoalParams): CompletableFuture<PlainTermGoal> {
        return languageServer.plainTermGoal(params)
    }

    fun rpcConnectAsync(params: RpcConnectParams): CompletableFuture<RpcConnected> {
        return languageServer.rpcConnect(params)
    }

    fun rpcCallAsync(params: RpcCallParams): CompletableFuture<JsonElement?> {
        return languageServer.rpcCall(params)
    }

    fun getInteractiveGoalsAsync(params: InteractiveGoalsParams) : CompletableFuture<InteractiveGoals> {
        return languageServer.rpcCall(params).thenApply {
            gson.fromJson(it, InteractiveGoals::class.java)
        }
    }

    fun infoToInteractiveAsync(params: InteractiveInfoParams) : CompletableFuture<CodeWithInfos> {
        return languageServer.rpcCall(params).thenApply {
            gson.fromJson(it, CodeWithInfos::class.java)
        }
    }

    fun rpcKeepAlive(params: RpcKeepAliveParams) {
        return languageServer.rpcKeepAlive(params)
    }

    companion object {
        val gson = GsonBuilder()
            .registerTypeAdapter(CodeWithInfos::class.java, object : JsonDeserializer<CodeWithInfos> {
                override fun deserialize(p0: JsonElement, p1: Type, p2: JsonDeserializationContext): CodeWithInfos? {
                    if (p0.isJsonObject && p0.asJsonObject.has("tag")) {
                        @Suppress("NAME_SHADOWING")
                        val p1 = p0.asJsonObject.getAsJsonArray("tag")

                        @Suppress("NAME_SHADOWING")
                        val f0: SubexprInfo = p2.deserialize(p1.get(0), SubexprInfo::class.java)
                        val f1: CodeWithInfos = p2.deserialize(p1.get(1), CodeWithInfos::class.java)
                        return CodeWithInfosTag(f0, f1)
                    }
                    if (p0.isJsonObject && p0.asJsonObject.has("append")) {
                        @Suppress("NAME_SHADOWING")
                        val p1 = p0.asJsonObject.getAsJsonArray("append")

                        @Suppress("NAME_SHADOWING")
                        val p2 = p2!!
                        val r: MutableList<CodeWithInfos> = ArrayList()
                        for (e in p1) {
                            r.add(p2.deserialize(e, CodeWithInfos::class.java))
                        }
                        return CodeWithInfosAppend(r)
                    }
                    if (p0.isJsonObject && p0.asJsonObject.has("text")) {
                        @Suppress("NAME_SHADOWING")
                        val p1 = p0.asJsonObject.getAsJsonPrimitive("text").asString
                        return CodeWithInfosText(p1)
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