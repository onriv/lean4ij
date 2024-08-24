package lean4ij.infoview.external

import com.google.gson.Gson
import lean4ij.infoview.external.data.CursorLocation
import lean4ij.infoview.external.data.InfoviewEvent
import lean4ij.lsp.data.Range
import lean4ij.lsp.data.RpcCallParamsRaw
import lean4ij.project.BuildWindowService
import lean4ij.project.LeanProjectService
import lean4ij.util.Constants
import lean4ij.util.OsUtil
import com.google.gson.JsonElement
import com.intellij.notification.BrowseNotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.ui.GotItTextBuilder
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.InitializeResult
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

/**
 * External infoview service, bridging the http service and the lean project service
 */
@Service(Service.Level.PROJECT)
class ExternalInfoViewService(val project: Project) {

    /**
     * using property rather than field for avoiding cyclic service injection
     */
    private val leanProjectService : LeanProjectService = project.service()

    /**
     * TODO this is in fact very sloppy using buildWindow...
     */
    private val buildWindowService : BuildWindowService = project.service()

    init {
        // TODO only for lean project
        if(leanProjectService.isLeanProject()) {
            startServer()
            leanProjectService.scope.launch {
                leanProjectService.caretEvent.collect {
                    val cursorLocation  = CursorLocation(it.textDocument.uri, Range(it.position, it.position))
                    previousCursorLocation = cursorLocation
                    val event = InfoviewEvent(Constants.EXTERNAL_INFOVIEW_CHANGED_CURSOR_LOCATION, cursorLocation)
                    // for (session in sessions) {
                    //     session.send(event)
                    // }
                    events.emit(event)
                }
            }
            leanProjectService.scope.launch {
                leanProjectService.serverEvent.collect {
                    // TODO the infoview app seems doing nothing with file progress
                    //      hence maybe skip it?
                    val event = InfoviewEvent("serverNotification", it)
                    // TODO does this require synchronization?
                    notificationMessages.add(event)
                    events.emit(event)
                }
            }
        }
    }

    /**
     * Here we create and start a Netty embedded server listening to the port
     * and define the main application module.
     * TODO make this port configurable or randomly chosen
     */
    private fun startServer() {
        thisLogger().info("starting external infoview service")
        val fakeFile = "externalInfoViewService"
        buildWindowService.startBuild(fakeFile)
        val module: Application.() -> Unit = {
            install(WebSockets)
            routing(externalInfoViewRoute(project, this@ExternalInfoViewService))
        }
        val port = OsUtil.findAvailableTcpPort()
        // val port = 19090

        // TODO only for debug, todo configuration it
        System.getProperty("idea.plugins.path")?.let {
            val projectRoot = System.getProperty("idea.plugins.path").replace(arrayOf("build", "idea-sandbox", "plugins").joinToString(File.separator), "")
            val file = Paths.get(projectRoot, "browser-infoview", "host-config.json").toFile()
            if (file.exists()) {
                val hostConfig = Gson().toJson(mapOf("host" to "localhost:$port"))
                file.writeText(hostConfig, StandardCharsets.UTF_8)
            }
        }

        val embeddedServer = embeddedServer(Netty, port = port, module = module)

        embeddedServer.start(wait = false)

        val url = "http://127.0.0.1:$port"
        val message = "infoview server start at $url"
        // this is copied from com/intellij/internal/ui/ShowGotItDemoAction.kt:239
        val tooltipMessage : GotItTextBuilder.() -> String = {
            buildString {
                append("infoview server start at ")
                append(browserLink(url, URL(url)))
            }
        }
        // TODO not easier to show though
        // GotItTooltip("externalInfoView", tooltipMessage).show()

        NotificationGroupManager.getInstance()
            .getNotificationGroup("Custom Notification Group")
            .createNotification("infoview server start at", NotificationType.INFORMATION)
            // .setListener(NotificationListener.URL_OPENING_LISTENER)
            .addAction(BrowseNotificationAction(url, url))
            .notify(project);

        buildWindowService.addBuildEvent(fakeFile, message)
        buildWindowService.endBuild(fakeFile)
        project.service<JcefInfoviewService>()
            .loadUrl(url)
    }

    private val events = MutableSharedFlow<InfoviewEvent>()

    /**
     * TODO here we send all old notificationMessages to new connections that
     *      starts after the server and the editor has been initialized
     *      it may cause by restarting the infoview only or starting
     *      a new tab in the browser
     *      Not sure if the infoview is designed in such a way or not, it's kind of lazy
     */
    val notificationMessages : MutableList<InfoviewEvent> = mutableListOf()

    /**
     * This is for showing the goal without moving the cursor at the startup
     * TODO this should be handled earlier
     */
    var previousCursorLocation : CursorLocation? = null

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


