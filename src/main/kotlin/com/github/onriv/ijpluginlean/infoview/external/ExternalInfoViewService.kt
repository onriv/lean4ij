package com.github.onriv.ijpluginlean.infoview.external

import com.github.onriv.ijpluginlean.infoview.external.data.CursorLocation
import com.github.onriv.ijpluginlean.infoview.external.data.InfoviewEvent
import com.github.onriv.ijpluginlean.infoview.external.data.SseEvent
import com.github.onriv.ijpluginlean.lsp.data.RpcCallParamsRaw
import com.github.onriv.ijpluginlean.lsp.data.Range
import com.github.onriv.ijpluginlean.project.LeanProjectService
import com.github.onriv.ijpluginlean.util.Constants
import com.github.onriv.ijpluginlean.util.OsUtil
import com.google.gson.JsonElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.collections.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.InitializeResult

/**
 * External infoview service, bridging the http service and the lean project service
 */
@Service(Service.Level.PROJECT)
class ExternalInfoViewService(val project: Project) {

    /**
     * using property rather than field for avoiding cyclic service injection
     */
    private val leanProjectService : LeanProjectService = project.service()

    init {
        startServer()
        leanProjectService.scope.launch {
            leanProjectService.caretEvent.collect {
                val cursorLocation  = CursorLocation(it.textDocument.uri, Range(it.position, it.position))
                val event = InfoviewEvent(Constants.EXTERNAL_INFOVIEW_CHANGED_CURSOR_LOCATION, cursorLocation)
                // for (session in sessions) {
                //     session.send(event)
                // }
                events.emit(event)
            }
        }
    }

    /**
     * Here we create and start a Netty embedded server listening to the port
     * and define the main application module.
     * TODO make this port configurable or randomly chosen
     */
    private fun startServer() {
        val module: Application.() -> Unit = {
            install(WebSockets)
            routing(externalInfoViewRoute(project, this@ExternalInfoViewService))
        }
        val port = OsUtil.findAvailableTcpPort()
        // val port = 19090
        val embeddedServer = embeddedServer(Netty, port = port, module = module)

        embeddedServer.start(wait = false)
        println("infoview server start at http://127.0.0.1:$port")
    }

    private val events = MutableSharedFlow<InfoviewEvent>()


    suspend fun awaitInitializedResult() : InitializeResult = leanProjectService.awaitInitializedResult()

    /**
     * events as flow
     */
    fun events(): Flow<InfoviewEvent> {
        return events.asSharedFlow()
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun getSession(uri: String) : String = leanProjectService.getSession(uri)

    suspend fun rpcCallRaw(params: RpcCallParamsRaw): JsonElement? {
        return leanProjectService.rpcCallRaw(params)
    }
}


