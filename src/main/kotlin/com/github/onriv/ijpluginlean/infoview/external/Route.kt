package com.github.onriv.ijpluginlean.infoview.external

import com.github.onriv.ijpluginlean.infoview.external.data.InfoviewEvent
import com.github.onriv.ijpluginlean.infoview.external.data.SseEvent
import com.github.onriv.ijpluginlean.lsp.LeanLanguageServer
import com.github.onriv.ijpluginlean.lsp.LeanLspServerManager
import com.github.onriv.ijpluginlean.lsp.data.PrcCallParamsRaw
import com.github.onriv.ijpluginlean.lsp.data.RpcCallParams
import com.github.onriv.ijpluginlean.util.Constants
import com.github.onriv.ijpluginlean.lsp.data.RpcConnectParams
import com.github.onriv.ijpluginlean.lsp.data.RpcConnected
import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.intellij.openapi.project.Project
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * copy from https://github.com/ktorio/ktor-samples/blob/main/sse/src/main/kotlin/io/ktor/samples/sse/SseApplication.kt
 * define all routes for external infoview
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun externalInfoViewRoute(project: Project, service : ExternalInfoViewService) : Routing.() -> Unit = {
    /**
     * an endpoint for testing if the server is up
     */
    get("/api") {
        call.respondText(
            "working"
        )
    }

    /**
     * Route to be executed when the client perform a GET `/api/sse/changedCursorLocation` request.
     * It will respond using the [respondSse] extension method
     * that uses the [SharedFlow] to collect sse events.
     */
    get("/api/sse") {
        val initializedEvent : Flow<SseEvent> = flow {
            val initializeResult = service.awaitInitializedResult()
            emit(SseEvent(InfoviewEvent(Constants.EXTERNAL_INFOVIEW_SERVER_INITIALIZED, initializeResult)))
        }
        call.respondSse(flowOf(initializedEvent, service.events()).flattenConcat())
    }

    post("/api/createRpcSession") {
        val params : RpcConnectParams = call.receiveJson()
        val session = service.getSession(params.uri)
        call.respondJson(RpcConnected(session))
    }

    post("/api/sendClientRequest") {
        val params: PrcCallParamsRaw = call.receiveJson()
        val ret = service.rpcCallRaw(params)
        if (ret == null) {
            // TODO better way to do this rather than using {}
            call.respondText("{}")
        } else {
            call.respondJson(ret)
        }
    }

}

private suspend inline fun <reified T> ApplicationCall.receiveJson(): T {
    return fromJson(receiveText())
}

private suspend fun ApplicationCall.respondJson(a: Any) {
    respond(toJson(a))
}
inline fun <reified T> fromJson(json: String) : T {
    return LeanLanguageServer.gson.fromJson(json, T::class.java)
}

fun toJsonElement(json: String): JsonElement {
    return LeanLanguageServer.gson.fromJson(json, JsonElement::class.java)
}

fun toJson(any: Any): String {
    return LeanLanguageServer.gson.toJson(any)
}