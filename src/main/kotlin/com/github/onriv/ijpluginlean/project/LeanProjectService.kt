package com.github.onriv.ijpluginlean.project

import com.github.onriv.ijpluginlean.infoview.external.ExternalInfoViewService
import com.github.onriv.ijpluginlean.lsp.InternalLeanLanguageServer
import com.github.onriv.ijpluginlean.lsp.LeanLanguageServer
import com.github.onriv.ijpluginlean.lsp.data.RpcConnectParams
import com.github.onriv.ijpluginlean.lsp.data.RpcConnected
import com.github.onriv.ijpluginlean.util.LspUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.services.LanguageServer

@Service(Service.Level.PROJECT)
class LeanProjectService(val project: Project, val scope: CoroutineScope)  {

    private var languageServer = CompletableDeferred<LeanLanguageServer>()
    private val initializeResult = CompletableDeferred<InitializeResult>()
    private val externalInfoViewService : ExternalInfoViewService = project.service()

    fun setInitializedServer(languageServer: LanguageServer) {
        this.languageServer.complete(LeanLanguageServer(languageServer as InternalLeanLanguageServer))
    }

    fun setInitializedResult(initializeResult: InitializeResult) {
        this.initializeResult.complete(initializeResult)
    }

    suspend fun awaitInitializedResult() : InitializeResult = initializeResult.await()

    fun getRelativePath(file: String): String {
        val unquotedFile = LspUtil.unquote(file)
        var prefix = project.basePath ?: return unquotedFile
        if (!prefix.endsWith("/")) {
            prefix += "/"
        }
        if (unquotedFile.startsWith(prefix)) {
            return unquotedFile.substring(prefix.length)
        }
        return unquotedFile
    }

    /**
     * TODO move to [LeanFile] for session lifecycle handling
     */
    suspend fun getSession(uri: String) : RpcConnected = languageServer.await().rpcConnect(RpcConnectParams(uri))


    // init {
        //
        //     var instance = LanguageServerLifecycleManager.getInstance(project)
        //     // kind of copying LanguageServerExplorerLifecycleListener
        //     // from lsp4ij
        //     instance.addLanguageServerLifecycleListener(object : LanguageServerLifecycleListener {
        //         override fun handleStatusChanged(p0: LanguageServerWrapper?) {
        //         }
        //
        //         override fun handleLSPMessage(p0: Message?, p1: MessageConsumer?, p2: LanguageServerWrapper?) {
        //             // p0.let {
        //             //     (it as? ResponseMessage)?.let {
        //             //         (it.result as? InitializeResult)?.let {
        //             //             serviceInitialized = Gson().toJson(it)
        //             //         }
        //             //     }
        //             // }
        //         }
        //
        //         override fun handleError(p0: LanguageServerWrapper?, p1: Throwable?) {
        //         }
        //
        //         override fun dispose() {
        //         }
        //
        //         })
        //     }
        // }
        //
        //
        // val servers = LanguageServiceAccessor.getInstance(project)
        //     .getActiveLanguageServers{true}
        // languageServer = LeanLanguageServer(servers[0] as InternalLeanLanguageServer)
    // }

}