package com.github.onriv.ijpluginlean.lsp

import com.github.onriv.ijpluginlean.lsp.data.PlainGoalParams
import com.github.onriv.ijpluginlean.lsp.data.PlainTermGoal
import com.github.onriv.ijpluginlean.lsp.data.PlainTermGoalParams
import com.github.onriv.ijpluginlean.services.ExternalInfoViewService
import com.intellij.openapi.components.service
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.*
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import java.io.File
import java.util.concurrent.CompletableFuture

internal class LeanLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        if (file.extension == "lean") {
            serverStarter.ensureServerStarted(LeanLspServerDescriptor(project))
        }
    }
}

private class LeanLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Lean") {

    private val gson = MessageJsonHandler(null).gson

    override fun isSupportedFile(file: VirtualFile) = file.extension == "lean"
    override fun createCommandLine() = GeneralCommandLine(
        // TODO configurable path for lake
        "~/.elan/toolchains/leanprover--lean4---v4.8.0-rc2/bin/lake".replaceFirst("~", System.getProperty("user.home"))
            .replace("/", File.separator),
        "serve", "--", project.basePath
    )
        // TODO handle this...
        // TODO don't know if this is necessary or not, but, any way vscode run it in project root
        .withWorkDirectory("~/GitRepo/mathematics_in_lean".replaceFirst("~", System.getProperty("user.home")))
        .withEnvironment("LEAN_SERVER_LOG_DIR", System.getProperty("user.home"))

    override val lspGoToDefinitionSupport: Boolean = true
    override val lspHoverSupport: Boolean = true

    override fun createInitializeParams(): InitializeParams {
        val ret = super.createInitializeParams()
        // see: https://leanprover.zulipchat.com/#narrow/stream/113488-general/topic/.E2.9C.94.20Lean.20LSP.20extensions/near/444888746
        // don't know what is used for though

        var capabilitiesStr = """
 {
        "workspace": {
            "applyEdit": true,
            "workspaceEdit": {
                "documentChanges": true,
                "resourceOperations": [
                    "create",
                    "rename",
                    "delete"
                ],
                "failureHandling": "textOnlyTransactional",
                "normalizesLineEndings": true,
                "changeAnnotationSupport": {
                    "groupsOnLabel": true
                }
            },
            "configuration": true,
            "didChangeWatchedFiles": {
                "dynamicRegistration": true,
                "relativePatternSupport": true
            },
            "symbol": {
                "dynamicRegistration": true,
                "symbolKind": {
                    "valueSet": [
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        10,
                        11,
                        12,
                        13,
                        14,
                        15,
                        16,
                        17,
                        18,
                        19,
                        20,
                        21,
                        22,
                        23,
                        24,
                        25,
                        26
                    ]
                },
                "tagSupport": {
                    "valueSet": [
                        1
                    ]
                },
                "resolveSupport": {
                    "properties": [
                        "location.range"
                    ]
                }
            },
            "codeLens": {
                "refreshSupport": true
            },
            "executeCommand": {
                "dynamicRegistration": true
            },
            "didChangeConfiguration": {
                "dynamicRegistration": true
            },
            "semanticTokens": {
                "refreshSupport": true
            },
            "fileOperations": {
                "dynamicRegistration": true,
                "didCreate": true,
                "didRename": true,
                "didDelete": true,
                "willCreate": true,
                "willRename": true,
                "willDelete": true
            },
            "inlineValue": {
                "refreshSupport": true
            },
            "inlayHint": {
                "refreshSupport": true
            },
            "diagnostics": {
                "refreshSupport": true
            }
        },
        "textDocument": {
            "publishDiagnostics": {
                "relatedInformation": true,
                "versionSupport": false,
                "tagSupport": {
                    "valueSet": [
                        1,
                        2
                    ]
                },
                "codeDescriptionSupport": true,
                "dataSupport": true
            },
            "synchronization": {
                "dynamicRegistration": true,
                "willSave": true,
                "willSaveWaitUntil": true,
                "didSave": true
            },
            "completion": {
                "dynamicRegistration": true,
                "contextSupport": true,
                "completionItem": {
                    "snippetSupport": true,
                    "commitCharactersSupport": true,
                    "documentationFormat": [
                        "markdown",
                        "plaintext"
                    ],
                    "deprecatedSupport": true,
                    "preselectSupport": true,
                    "tagSupport": {
                        "valueSet": [
                            1
                        ]
                    },
                    "insertReplaceSupport": true,
                    "resolveSupport": {
                        "properties": [
                            "documentation",
                            "detail",
                            "additionalTextEdits"
                        ]
                    },
                    "insertTextModeSupport": {
                        "valueSet": [
                            1,
                            2
                        ]
                    },
                    "labelDetailsSupport": true
                },
                "insertTextMode": 2,
                "completionItemKind": {
                    "valueSet": [
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        10,
                        11,
                        12,
                        13,
                        14,
                        15,
                        16,
                        17,
                        18,
                        19,
                        20,
                        21,
                        22,
                        23,
                        24,
                        25
                    ]
                },
                "completionList": {
                    "itemDefaults": [
                        "commitCharacters",
                        "editRange",
                        "insertTextFormat",
                        "insertTextMode"
                    ]
                }
            },
            "hover": {
                "dynamicRegistration": true,
                "contentFormat": [
                    "markdown",
                    "plaintext"
                ]
            },
            "signatureHelp": {
                "dynamicRegistration": true,
                "signatureInformation": {
                    "documentationFormat": [
                        "markdown",
                        "plaintext"
                    ],
                    "parameterInformation": {
                        "labelOffsetSupport": true
                    },
                    "activeParameterSupport": true
                },
                "contextSupport": true
            },
            "definition": {
                "dynamicRegistration": true,
                "linkSupport": true
            },
            "references": {
                "dynamicRegistration": true
            },
            "documentHighlight": {
                "dynamicRegistration": true
            },
            "documentSymbol": {
                "dynamicRegistration": true,
                "symbolKind": {
                    "valueSet": [
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        10,
                        11,
                        12,
                        13,
                        14,
                        15,
                        16,
                        17,
                        18,
                        19,
                        20,
                        21,
                        22,
                        23,
                        24,
                        25,
                        26
                    ]
                },
                "hierarchicalDocumentSymbolSupport": true,
                "tagSupport": {
                    "valueSet": [
                        1
                    ]
                },
                "labelSupport": true
            },
            "codeAction": {
                "dynamicRegistration": true,
                "isPreferredSupport": true,
                "disabledSupport": true,
                "dataSupport": true,
                "resolveSupport": {
                    "properties": [
                        "edit"
                    ]
                },
                "codeActionLiteralSupport": {
                    "codeActionKind": {
                        "valueSet": [
                            "",
                            "quickfix",
                            "refactor",
                            "refactor.extract",
                            "refactor.inline",
                            "refactor.rewrite",
                            "source",
                            "source.organizeImports"
                        ]
                    }
                },
                "honorsChangeAnnotations": false
            },
            "codeLens": {
                "dynamicRegistration": true
            },
            "formatting": {
                "dynamicRegistration": true
            },
            "rangeFormatting": {
                "dynamicRegistration": true
            },
            "onTypeFormatting": {
                "dynamicRegistration": true
            },
            "rename": {
                "dynamicRegistration": true,
                "prepareSupport": true,
                "prepareSupportDefaultBehavior": 1,
                "honorsChangeAnnotations": true
            },
            "documentLink": {
                "dynamicRegistration": true,
                "tooltipSupport": true
            },
            "typeDefinition": {
                "dynamicRegistration": true,
                "linkSupport": true
            },
            "implementation": {
                "dynamicRegistration": true,
                "linkSupport": true
            },
            "colorProvider": {
                "dynamicRegistration": true
            },
            "foldingRange": {
                "dynamicRegistration": true,
                "rangeLimit": 5000,
                "lineFoldingOnly": true,
                "foldingRangeKind": {
                    "valueSet": [
                        "comment",
                        "imports",
                        "region"
                    ]
                },
                "foldingRange": {
                    "collapsedText": false
                }
            },
            "declaration": {
                "dynamicRegistration": true,
                "linkSupport": true
            },
            "selectionRange": {
                "dynamicRegistration": true
            },
            "callHierarchy": {
                "dynamicRegistration": true
            },
            "semanticTokens": {
                "dynamicRegistration": true,
                "tokenTypes": [
                    "namespace",
                    "type",
                    "class",
                    "enum",
                    "interface",
                    "struct",
                    "typeParameter",
                    "parameter",
                    "variable",
                    "property",
                    "enumMember",
                    "event",
                    "function",
                    "method",
                    "macro",
                    "keyword",
                    "modifier",
                    "comment",
                    "string",
                    "number",
                    "regexp",
                    "operator",
                    "decorator"
                ],
                "tokenModifiers": [
                    "declaration",
                    "definition",
                    "readonly",
                    "static",
                    "deprecated",
                    "abstract",
                    "async",
                    "modification",
                    "documentation",
                    "defaultLibrary"
                ],
                "formats": [
                    "relative"
                ],
                "requests": {
                    "range": true,
                    "full": {
                        "delta": true
                    }
                },
                "multilineTokenSupport": false,
                "overlappingTokenSupport": false,
                "serverCancelSupport": true,
                "augmentsSyntaxTokens": true
            },
            "linkedEditingRange": {
                "dynamicRegistration": true
            },
            "typeHierarchy": {
                "dynamicRegistration": true
            },
            "inlineValue": {
                "dynamicRegistration": true
            },
            "inlayHint": {
                "dynamicRegistration": true,
                "resolveSupport": {
                    "properties": [
                        "tooltip",
                        "textEdits",
                        "label.tooltip",
                        "label.location",
                        "label.command"
                    ]
                }
            },
            "diagnostic": {
                "dynamicRegistration": true,
                "relatedDocumentSupport": false
            }
        },
        "window": {
            "showMessage": {
                "messageActionItem": {
                    "additionalPropertiesSupport": true
                }
            },
            "showDocument": {
                "support": true
            },
            "workDoneProgress": true
        },
        "general": {
            "staleRequestSupport": {
                "cancel": true,
                "retryOnContentModified": [
                    "textDocument/semanticTokens/full",
                    "textDocument/semanticTokens/range",
                    "textDocument/semanticTokens/full/delta"
                ]
            },
            "regularExpressions": {
                "engine": "ECMAScript",
                "version": "ES2020"
            },
            "markdown": {
                "parser": "marked",
                "version": "1.1.0"
            },
            "positionEncodings": [
                "utf-16"
            ]
        },
        "notebookDocument": {
            "synchronization": {
                "dynamicRegistration": true,
                "executionSummarySupport": true
            }
        }
    }           
        """
        ret.capabilities = gson.fromJson(capabilitiesStr, ClientCapabilities::class.java)


        return ret
    }

    override fun getLanguageId(file: VirtualFile): String {
        // This is get from comparing vscode's lsp trace log
        // TODO don't know if there more robust way to do it, or if it's relavant
        return "lean4"
    }



    /**
     * copied from https://github.com/tomblachut/svelte-intellij/blob/master/src/main/java/dev/blachut/svelte/lang/service/SvelteLspServerSupportProvider.kt
     * and
     * https://github.com/huggingface/llm-intellij/blob/main/src/main/kotlin/co/huggingface/llmintellij/lsp/LlmLsLspServerDescriptor.kt#L31
     * it differs with the document:
     * https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html#customization
     */
    override val lsp4jServerClass: Class<out LanguageServer> = LeanLanguageServer::class.java

    override fun createLsp4jClient(handler: LspServerNotificationsHandler): Lsp4jClient {
        return LeanLsp4jClient(handler);
    }

    override val lspServerListener: LspServerListener = LeanLspServerListener(project)

}

class RpcConnectParams(
    val uri: String
)

/**
 * Lean declares sessionId as UInt32, it's kind of hard to work with in Java/Kotlin
 * like: 17710504432720554099 exit long
 * see: src/Lean/Data/Lsp/Extra.lean:124 to
 */
class RpcConnected(
    val sessionId: String
)

/**
/-- `$/lean/plainGoal` client<-server reply. -/
structure PlainGoal where
/-- The goals as pretty-printed Markdown, or something like "no goals" if accomplished. -/
rendered : String
/-- The pretty-printed goals, empty if all accomplished. -/
goals : Array String
deriving FromJson, ToJson
 */
class PlainGoal(
    val rendered: String,
    val goals: List<String>
)

/** there are two structures of range */
class Range(
    val start: Position,
    val end: Position,
)

class ProcessingInfo(
    val range: Range,
    val kind: Int
)

/**
structure LeanFileProgressProcessingInfo where
range : Range
kind : LeanFileProgressKind := LeanFileProgressKind.processing
deriving FromJson, ToJson
*/
class LeanFileProgressProcessingInfo(
    val textDocument: TextDocumentIdentifier,
    val processing: List<ProcessingInfo>
)

/**
 * The serialization is using Json
 * 2024-06-23 15:45:15,210 [  37428] SEVERE - org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer - Message could not be parsed.
 * org.eclipse.lsp4j.jsonrpc.MessageIssueException: Message could not be parsed.
 * 	at org.eclipse.lsp4j.jsonrpc.json.adapters.MessageTypeAdapter.read(MessageTypeAdapter.java:146)
 * 	at org.eclipse.lsp4j.jsonrpc.json.adapters.MessageTypeAdapter.read(MessageTypeAdapter.java:56)
 * 	at com.google.gson.Gson.fromJson(Gson.java:1227)
 * 	at com.google.gson.Gson.fromJson(Gson.java:1186)
 * 	at org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler.parseMessage(MessageJsonHandler.java:119)
 * 	at org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler.parseMessage(MessageJsonHandler.java:114)
 * 	at com.intellij.platform.lsp.impl.connector.Lsp4jServerConnector$createMessageJsonHandler$1.parseMessage(Lsp4jServerConnector.kt:149)
 * 	at org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer.handleMessage(StreamMessageProducer.java:193)
 * 	at org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer.listen(StreamMessageProducer.java:94)
 * 	at com.intellij.platform.lsp.impl.connector.Lsp4jServerConnector.m(Lsp4jServerConnector.kt:65)
 * 	at com.intellij.util.ConcurrencyUtil.runUnderThreadName(ConcurrencyUtil.java:218)
 * 	at com.intellij.platform.lsp.impl.connector.Lsp4jServerConnector.U(Lsp4jServerConnector.kt:61)
 * 	at com.intellij.openapi.application.impl.RwLockHolder$executeOnPooledThread$1.run(RwLockHolder.kt:154)
 * 	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:539)
 * 	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
 * 	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
 * 	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 * 	at java.base/java.util.concurrent.Executors$PrivilegedThreadFactory$1$1.run(Executors.java:702)
 * 	at java.base/java.util.concurrent.Executors$PrivilegedThreadFactory$1$1.run(Executors.java:699)
 * 	at java.base/java.security.AccessController.doPrivileged(AccessController.java:399)
 * 	at java.base/java.util.concurrent.Executors$PrivilegedThreadFactory$1.run(Executors.java:699)
 * 	at java.base/java.lang.Thread.run(Thread.java:840)
 */
internal interface LeanLanguageServer : LanguageServer, TextDocumentService {

    @JsonRequest("\$/lean/plainGoal")
    fun plainGoal(params: PlainGoalParams): CompletableFuture<PlainGoal>

    @JsonRequest("\$/lean/plainTermGoal")
    fun plainTermGoal(params: PlainTermGoalParams): CompletableFuture<PlainTermGoal>

    /**
     * /-- `$/lean/rpc/connect` client->server request.
     *
     * Starts an RPC session at the given file's worker, replying with the new session ID.
     * Multiple sessions may be started and operating concurrently.
     *
     * A session may be destroyed by the server at any time (e.g. due to a crash), in which case further
     * RPC requests for that session will reply with `RpcNeedsReconnect` errors. The client should discard
     * references held from that session and `connect` again. -/
     * ref: https://github.com/leanprover/lean4/blob/6b93f05cd172640253ad1ed27935167e5a3af981/src/Lean/Data/Lsp/Extra.lean
     */
    @JsonRequest("\$/lean/rpc/connect")
    fun rpcConnect(params: RpcConnectParams): CompletableFuture<RpcConnected>

    @JsonRequest("\$/lean/rpc/call")
    fun rpcCall(params: Any): CompletableFuture<Any?>


}

class LeanLsp4jClient(serverNotificationsHandler: LspServerNotificationsHandler) :
    Lsp4jClient(serverNotificationsHandler) {

    @JsonNotification("\$/lean/fileProgress")
    fun leanFileProgress(params: LeanFileProgressProcessingInfo) {
        FileProgress.run(params)
    }

}

class LeanLspServerListener (val project: Project): LspServerListener {
    private val gson = MessageJsonHandler(null).gson
    private val infoViewService = project.service<ExternalInfoViewService>()

    override fun serverInitialized(params: InitializeResult) {
        infoViewService.serviceInitialized = gson.toJson(params)
        // runBlocking {infoViewService.send(gson.toJson(params))}
    }
}