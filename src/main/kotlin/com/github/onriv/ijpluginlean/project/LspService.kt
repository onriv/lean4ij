package com.github.onriv.ijpluginlean.project

import com.github.onriv.ijpluginlean.lsp.InternalLeanLanguageServer
import com.github.onriv.ijpluginlean.lsp.LeanLanguageServer
import com.github.onriv.ijpluginlean.lsp.LeanLspServerManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerWrapper
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor
import com.redhat.devtools.lsp4ij.lifecycle.LanguageServerLifecycleListener
import com.redhat.devtools.lsp4ij.lifecycle.LanguageServerLifecycleManager
import kotlinx.coroutines.CoroutineScope
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.jsonrpc.MessageConsumer
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage
import org.eclipse.lsp4j.services.LanguageServer

@Service(Service.Level.PROJECT)
class LspService(project: Project,  val scope: CoroutineScope)  {

    companion object {
        fun instance(project: Project): LspService = project.service()
    }

    private var languageServer: LeanLanguageServer? = null
    private var initializeResult : InitializeResult? = null

    fun setInitializedServer(languageServer: LanguageServer) {
        this.languageServer = LeanLanguageServer(languageServer as InternalLeanLanguageServer)
    }

    fun setInitializedResult(initializeResult: InitializeResult) {
        this.initializeResult = initializeResult
    }


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