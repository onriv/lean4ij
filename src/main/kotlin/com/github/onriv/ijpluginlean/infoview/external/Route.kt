package com.github.onriv.ijpluginlean.infoview.external

import com.github.onriv.ijpluginlean.infoview.external.data.InfoviewEvent
import com.github.onriv.ijpluginlean.infoview.external.data.SseEvent
import com.github.onriv.ijpluginlean.lsp.LeanLanguageServer
import com.github.onriv.ijpluginlean.lsp.data.PrcCallParamsRaw
import com.github.onriv.ijpluginlean.lsp.data.RpcConnectParams
import com.github.onriv.ijpluginlean.lsp.data.RpcConnected
import com.github.onriv.ijpluginlean.project.LeanProjectService
import com.github.onriv.ijpluginlean.util.Constants
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.squareup.wire.durationOfSeconds
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.util.Identity.encode
import io.ktor.util.collections.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.lang.Runnable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration

/**
 * copy from https://github.com/ktorio/ktor-samples/blob/main/sse/src/main/kotlin/io/ktor/samples/sse/SseApplication.kt
 * define all routes for external infoview
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun externalInfoViewRoute(project: Project, service : ExternalInfoViewService) : RootRoute.() -> Unit = {

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

    sse("/api/sse") {
        val initializeResult = service.awaitInitializedResult()
        send(Gson().toJson(InfoviewEvent(Constants.EXTERNAL_INFOVIEW_SERVER_INITIALIZED, initializeResult)))
        service.events().collect {
            send(Gson().toJson(it.data))
        }
    }

    /**
     * This is temporally for sse bug
     */
    get("/api/serverRestarted") {
        val initializeResult = service.awaitInitializedResult()
        call.respondJson(initializeResult)
    }

    /**
     * This is temporally for sse bug
     */
    val sessions = ConcurrentSet<Channel<SseEvent>>()
    project.service<LeanProjectService>().scope.launch {
        service.events().collect {
            for (session in sessions) {
                session.send(it)
            }
        }
    }

    /**
     * This is temporally for sse bug
     */
    get("/api/poll") {
        val channel = Channel<SseEvent>()
        sessions.add(channel)
        val event = channel.receive()
        sessions.remove(channel)
        call.respondJson(event)
    }

    post("/api/createRpcSession") {
        withContext(Dispatchers.IO) {
            val params : RpcConnectParams = call.receiveJson()
            val session = service.getSession(params.uri)
            call.respondJson(RpcConnected(session))
        }
    }

    post("/api/sendClientRequest") {
        withContext(Dispatchers.IO) {
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