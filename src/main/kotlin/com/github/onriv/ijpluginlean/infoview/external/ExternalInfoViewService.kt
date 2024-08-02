package com.github.onriv.ijpluginlean.infoview.external

import com.github.onriv.ijpluginlean.infoview.external.data.CursorLocation
import com.github.onriv.ijpluginlean.infoview.external.data.InfoviewEvent
import com.github.onriv.ijpluginlean.infoview.external.data.SseEvent
import com.github.onriv.ijpluginlean.lsp.data.PrcCallParamsRaw
import com.github.onriv.ijpluginlean.lsp.data.Range
import com.github.onriv.ijpluginlean.lsp.data.RpcConnected
import com.github.onriv.ijpluginlean.util.Constants
import com.github.onriv.ijpluginlean.project.LeanProjectService
import com.google.gson.JsonElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
                events.emit(SseEvent(InfoviewEvent(Constants.EXTERNAL_INFOVIEW_CHANGED_CURSOR_LOCATION, cursorLocation)))
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
            routing(externalInfoViewRoute(project, this@ExternalInfoViewService))
        }
        val embeddedServer = embeddedServer(Netty, port = 19094, module = module)
        embeddedServer.start(wait = false)
    }

    private val events = MutableSharedFlow<SseEvent>()

    suspend fun awaitInitializedResult() : InitializeResult = leanProjectService.awaitInitializedResult()

    /**
     * events as flow
     */
    fun events(): Flow<SseEvent> {
        return events.asSharedFlow()
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * send a sse event
     */
    private fun send(event: SseEvent) {
        scope.launch {
            events.emit(event)
        }
    }

    fun changedCursorLocation(cursorLocation: CursorLocation) {
        send(SseEvent(InfoviewEvent(Constants.EXTERNAL_INFOVIEW_CHANGED_CURSOR_LOCATION, cursorLocation)))
    }

    suspend fun getSession(uri: String) : String = leanProjectService.getSession(uri)

    suspend fun rpcCallRaw(params: PrcCallParamsRaw): JsonElement? {
        return leanProjectService.rpcCallRaw(params)
    }
}


