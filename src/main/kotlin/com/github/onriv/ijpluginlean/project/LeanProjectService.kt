package com.github.onriv.ijpluginlean.project

import com.github.onriv.ijpluginlean.lsp.InternalLeanLanguageServer
import com.github.onriv.ijpluginlean.lsp.LeanLanguageServer
import com.github.onriv.ijpluginlean.lsp.data.PlainGoalParams
import com.github.onriv.ijpluginlean.lsp.data.PrcCallParamsRaw
import com.github.onriv.ijpluginlean.lsp.data.RpcConnectParams
import com.github.onriv.ijpluginlean.lsp.data.RpcConnected
import com.github.onriv.ijpluginlean.util.LspUtil
import com.google.gson.JsonElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class LeanProjectService(val project: Project, val scope: CoroutineScope)  {

    val languageServer = CompletableDeferred<LeanLanguageServer>()
    private val initializeResult = CompletableDeferred<InitializeResult>()

    private val _caretEvent = MutableSharedFlow<PlainGoalParams>()
    val caretEvent: Flow<PlainGoalParams> get() = _caretEvent.asSharedFlow()

    private val leanFiles = ConcurrentHashMap<String, LeanFile>()

    fun file(file: VirtualFile): LeanFile {
        return file(LspUtil.quote(file.path))
    }

    fun file(file: String) : LeanFile {
        return leanFiles.computeIfAbsent(file) {LeanFile(this, file)}
    }

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
    suspend fun getSession(uri: String) : String = file(uri).getSession()

    fun updateCaret(params: PlainGoalParams) {
        scope.launch {
            _caretEvent.emit(params)
        }
    }

    suspend fun rpcCallRaw(params: PrcCallParamsRaw): JsonElement? {
        return file(params.textDocument.uri).rpcCallRaw(params)
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