package com.github.onriv.ijpluginlean.lsp

import com.github.onriv.ijpluginlean.project.LeanProjectService
import com.github.onriv.ijpluginlean.util.Constants
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerWrapper
import com.redhat.devtools.lsp4ij.lifecycle.LanguageServerLifecycleListener
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.jsonrpc.MessageConsumer
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage

class LeanLanguageServerLifecycleListener(val project: Project) : LanguageServerLifecycleListener {
    private val leanProjectService : LeanProjectService = project.service()

    override fun handleStatusChanged(languageServer: LanguageServerWrapper) {
        if (languageServer.serverDefinition.id != Constants.LEAN_LANGUAGE_SERVER_ID) {
            return
        }
        languageServer.initializedServer.thenAccept { leanProjectService.setInitializedServer(it) }
    }

    override fun handleLSPMessage(message: Message, consumer: MessageConsumer, languageServer: LanguageServerWrapper) {
        if (languageServer.serverDefinition.id != Constants.LEAN_LANGUAGE_SERVER_ID) {
            return
        }
        (message as? ResponseMessage)?.let { (it.result as? InitializeResult)?.let {it2 ->
            leanProjectService.setInitializedResult(it2)
        }}
    }

    override fun handleError(p0: LanguageServerWrapper?, p1: Throwable?) {
    }

    override fun dispose() {
    }

}
