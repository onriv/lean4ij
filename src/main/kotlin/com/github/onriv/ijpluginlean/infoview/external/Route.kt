package com.github.onriv.ijpluginlean.infoview.external

import com.github.onriv.ijpluginlean.infoview.external.data.InfoviewEvent
import com.github.onriv.ijpluginlean.infoview.external.data.SseEvent
import com.github.onriv.ijpluginlean.lsp.LeanLanguageServer
import com.github.onriv.ijpluginlean.lsp.data.RpcCallParamsRaw
import com.github.onriv.ijpluginlean.lsp.data.RpcConnectParams
import com.github.onriv.ijpluginlean.lsp.data.RpcConnected
import com.github.onriv.ijpluginlean.util.Constants
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.intellij.openapi.project.Project
import com.jetbrains.rd.framework.util.withContext
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import java.lang.Runnable
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * copy from https://github.com/ktorio/ktor-samples/blob/main/sse/src/main/kotlin/io/ktor/samples/sse/SseApplication.kt
 * define all routes for external infoview
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun externalInfoViewRoute(project: Project, service : ExternalInfoViewService) : Route.() -> Unit = {

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
    // get("/api/sse") {
    //     withContext(Dispatchers.IO) {
    //         val initializedEvent : Flow<SseEvent> = flow {
    //             val initializeResult = service.awaitInitializedResult()
    //             emit(SseEvent(InfoviewEvent(Constants.EXTERNAL_INFOVIEW_SERVER_INITIALIZED, initializeResult)))
    //         }
    //         call.respondSse(flowOf(initializedEvent, service.events()).flattenConcat())
    //     }
    // }

    val customScope = CoroutineScope(Executors.newFixedThreadPool(4, object : ThreadFactory {
        private val counter = AtomicInteger(0)
        override fun newThread(r: Runnable): Thread {
            val thread = Thread()
            thread.name = "SSE Thread ${counter.getAndIncrement()}"
            return thread
        }
    }).asCoroutineDispatcher())

    webSocket("/ws") {
        // send(Frame.Text("connected"))
        val outgoingJob = launch {
            val serverRestarted = service.awaitInitializedResult()
            send(Frame.Text(Gson().toJson(InfoviewEvent("serverRestarted", serverRestarted))))
            service.events().collect {
                send(Frame.Text(Gson().toJson(it)))
            }
        }
        runCatching {
            while (true) {
                // TODO the original example in https://ktor.io/docs/server-websockets.html#handle-multiple-session
                //      is using consumeEach, but I am not familiar with it
                val frame = incoming.receive()
                if (frame is Frame.Text) {
                    val (requestId, method, data) = frame.readText().split(Regex(","), 3)
                    if (method == "createRpcSession") {
                        launch {
                            val params: RpcConnectParams = fromJson(data)
                            val session = service.getSession(params.uri)
                            val resp =
                                mapOf("requestId" to requestId.toInt(), "method" to "rpcResponse", "data" to session)
                            send(Gson().toJson(resp))
                        }
                    }
                    if (method == "sendClientRequest") {
                        launch {
                            val params: RpcCallParamsRaw = fromJson(data)
                            val ret = service.rpcCallRaw(params)
                            val resp = mapOf("requestId" to requestId.toInt(), "method" to "rpcResponse", "data" to ret)
                            send(Gson().toJson(resp))
                        }
                    }
                }
            }
        }.onFailure { exception ->
            // TODO handle it seriously
            // TODO seems cannot throw?
            exception.cause!!.cause!!.printStackTrace()
            throw exception
        }.also {
            // TODO check what also means
            outgoingJob.cancel()
        }
        outgoingJob.join()
    }

    /**
     * This is temporally for sse bug
     */
    get("/api/serverRestarted") {
        val initializeResult = service.awaitInitializedResult()
        call.respondJson(initializeResult)
    }

    post("/api/createRpcSession") {
        val params : RpcConnectParams = call.receiveJson()
        val session = service.getSession(params.uri)
        call.respondJson(RpcConnected(session))
    }

    post("/api/sendClientRequest") {
        val params: RpcCallParamsRaw = call.receiveJson()
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