package com.github.onriv.ijpluginlean.infoview.external

import com.github.onriv.ijpluginlean.infoview.external.data.InfoviewEvent
import com.github.onriv.ijpluginlean.infoview.external.data.SseEvent
import com.github.onriv.ijpluginlean.lsp.LeanLspServerManager
import com.github.onriv.ijpluginlean.util.Constants
import com.github.onriv.ijpluginlean.lsp.data.RpcConnectParams
import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
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
        val params = call.receive(RpcConnectParams::class)
        val session = service.getSession(params.uri)
        call.respond(session)
    }

    post("/api/sendClientRequest") {
        val text = call.receiveText()
        // TODO async way? kotlin way?
        // val resp = LeanLspServerManager.getInstance(project = project).rpcCallRaw(gson.fromJson(text, Any::class.java))
        // if (resp == null) {
        //     call.respondText("{}")
        // } else {
        //     call.respondText {
        //         Gson().toJson(resp)
        //     }
        // }
    }

}