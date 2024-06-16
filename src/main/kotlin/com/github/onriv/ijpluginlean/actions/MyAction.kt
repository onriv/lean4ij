package com.github.onriv.ijpluginlean.actions

import com.github.onriv.ijpluginlean.lsp.LeanLanguageServer
import com.github.onriv.ijpluginlean.lsp.LeanLspServerSupportProvider
import com.github.onriv.ijpluginlean.lsp.RpcConnectParams
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class MyCustomAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        // Display a dialog with 'Hello World!'
        Messages.showMessageDialog("Hello World!", "Greeting", Messages.getInformationIcon())
    }
}

class OpenLeanInfoView : AnAction() {
    private val sessions = ConcurrentHashMap<String, String>()

    override fun actionPerformed(e: AnActionEvent) {
        val currentFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.project?.let { project -> currentFile?.let {file ->
            runBlocking {
                rpcConnect(project, file).collect { value ->
//                    sessions[file] = value.
                    println(value)
                }
            }
        }}
    }

    // copy from https://github.com/huggingface/llm-intellij/blob/main/src/main/kotlin/co/huggingface/llmintellij/LlmLsCompletionProvider.kt#L42
    // TODO better way and pos for this?
    fun rpcConnect(project: Project, file: VirtualFile): Flow<Long> = channelFlow {
        val lspServer = LspServerManager.getInstance(project)
            .getServersForProvider(LeanLspServerSupportProvider::class.java).firstOrNull()
        if (lspServer != null) {
            val resp = lspServer.sendRequest { (it as LeanLanguageServer).rpcConnect(RpcConnectParams(file.url)) }
            resp?.let{r -> send(r.sessionId)}
        }
        awaitClose()
    }

}