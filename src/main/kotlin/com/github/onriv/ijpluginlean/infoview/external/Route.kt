package com.github.onriv.ijpluginlean.infoview.external

import com.github.onriv.ijpluginlean.infoview.external.data.InfoviewEvent
import com.github.onriv.ijpluginlean.infoview.external.data.SseEvent
import com.github.onriv.ijpluginlean.lsp.LeanLanguageServer
import com.github.onriv.ijpluginlean.lsp.data.PrcCallParamsRaw
import com.github.onriv.ijpluginlean.lsp.data.RpcConnectParams
import com.github.onriv.ijpluginlean.lsp.data.RpcConnected
import com.github.onriv.ijpluginlean.util.Constants
import com.google.gson.JsonElement
import com.intellij.openapi.project.Project
import io.ktor.server.application.*
import io.ktor.server.http.content.*
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
     * see: https://ktor.io/docs/server-serving-spa.html#serve-customize
     * and https://ktor.io/docs/server-static-content.html
     */
    singlePageApplication {
        useResources = true
        ignoreFiles {
            val pathSegments = it.split(".jar!")
            // this is for handling path in jar file:
            // it seems that plugins are packaged like plugins/.../jar!/index.hmlt
            val path = pathSegments[pathSegments.size-1]
            if (path.startsWith("/assets")) {
                return@ignoreFiles false
            }
            if (path.startsWith("/fonts")) {
                return@ignoreFiles false
            }
            if (path == "/index.html") {
                return@ignoreFiles false
            }
            // TODO remove this
            if (path == "/vite.svg") {
                return@ignoreFiles false
            }
            true
        }
    }


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