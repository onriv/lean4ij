package com.github.onriv.ijpluginlean.lsp

import com.github.onriv.ijpluginlean.project.LspService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerWrapper
import com.redhat.devtools.lsp4ij.lifecycle.LanguageServerLifecycleListener
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.jsonrpc.MessageConsumer
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage

class LeanLanguageServerLifecycleListener(val project: Project) : LanguageServerLifecycleListener {
    private val lspService = LspService.instance(project)

    override fun handleStatusChanged(languageServer: LanguageServerWrapper) {
        if (languageServer.serverDefinition.id != LspConstants.LEAN_LANGUAGE_SERVER_ID) {
            return
        }
        languageServer.initializedServer.thenAccept { lspService.setInitializedServer(it) }
    }

    override fun handleLSPMessage(message: Message, consumer: MessageConsumer, languageServer: LanguageServerWrapper) {
        if (languageServer.serverDefinition.id != LspConstants.LEAN_LANGUAGE_SERVER_ID) {
            return
        }
        (message as? ResponseMessage)?.let { (it.result as? InitializeResult)?.let {it2 ->
            lspService.setInitializedResult(it2)
        }}
    }

    override fun handleError(p0: LanguageServerWrapper?, p1: Throwable?) {
    }

    override fun dispose() {
    }

}
