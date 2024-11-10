package lean4ij.project

import lean4ij.lsp.InternalLeanLanguageServer
import lean4ij.lsp.LeanLanguageServer
import lean4ij.lsp.data.PlainGoalParams
import lean4ij.lsp.data.RpcCallParamsRaw
import lean4ij.util.Constants
import lean4ij.util.LspUtil
import com.google.gson.JsonElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.LineColumn
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.redhat.devtools.lsp4ij.LanguageServerManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import lean4ij.util.LeanUtil
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage
import org.eclipse.lsp4j.services.LanguageServer
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path

@Service(Service.Level.PROJECT)
class LeanProjectService(val project: Project, val scope: CoroutineScope)  {

    private var _languageServer = CompletableDeferred<LeanLanguageServer>()
    val languageServer : CompletableDeferred<LeanLanguageServer> get() = _languageServer
    private var _initializeResult = CompletableDeferred<InitializeResult>()
    private val initializeResult : CompletableDeferred<InitializeResult> = _initializeResult

    private val _caretEvent = MutableSharedFlow<PlainGoalParams>()
    val caretEvent: Flow<PlainGoalParams> get() = _caretEvent.asSharedFlow()
    private val _serverEvent = MutableSharedFlow<NotificationMessage>()
    val serverEvent : Flow<NotificationMessage> get() = _serverEvent.asSharedFlow()

    fun emitServerEvent(message: NotificationMessage) {
        scope.launch {
            _serverEvent.emit(message)
        }
    }

    private val leanFiles = ConcurrentHashMap<String, LeanFile>()

    fun file(file: VirtualFile): LeanFile {
        val ret = file(LspUtil.quote(file.path))
        ret.virtualFile = file
        return ret
    }

    fun file(file: String) : LeanFile {
        return leanFiles.computeIfAbsent(file) { LeanFile(this, file) }
    }

    fun setInitializedServer(languageServer: LanguageServer) {
        val result = this.languageServer.complete(LeanLanguageServer(languageServer as InternalLeanLanguageServer))
        if (!result) {
            // TODO there is still multiple event
            // throw IllegalStateException("languageServer already setup")
            thisLogger().warn("languageServer already setup")
        }
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

    suspend fun rpcCallRaw(params: RpcCallParamsRaw): JsonElement? {
        return file(params.textDocument.uri).rpcCallRaw(params)
    }

    fun restartLsp() {
        // TODO should this be lock?
        _initializeResult = CompletableDeferred()
        _languageServer = CompletableDeferred()
        LanguageServerManager.getInstance(project).stop(Constants.LEAN_LANGUAGE_SERVER_ID)
        LanguageServerManager.getInstance(project).start(Constants.LEAN_LANGUAGE_SERVER_ID)
    }

    fun resetServer() {
        // TODO should this be lock?
        _initializeResult = CompletableDeferred()
        _languageServer = CompletableDeferred()
    }

    /**
     * TODO [lean4ij.lsp.LeanLanguageServerProvider.setServerCommand] contains some duplicated logic for this
     */
    fun isLeanProject(): Boolean {
        return Path(project.basePath!!, "lean-toolchain").toFile().isFile
    }

    fun updateInfoviewFor(document: Document) {
        val file = FileDocumentManager.getInstance().getFile(document)?:return
        if (!LeanUtil.isLeanFile(file)) return
        val editor = EditorFactory.getInstance().getEditors(document).firstOrNull()?:return
        val lineCol : LineColumn = StringUtil.offsetToLineColumn(document.text, editor.caretModel.offset) ?: return
        val position = LogicalPosition(lineCol.line, lineCol.column)
        // TODO this may be duplicated with caret events some times
        //      but without this there are cases no caret events but document changed events
        //      maybe some debounce
        file(file).updateCaret(editor, position)
    }

    fun updateInfoviewFor(editor: Editor, forceUpdate: Boolean = false) {
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document)?:return
        if (!LeanUtil.isLeanFile(file)) return
        val lineCol : LineColumn = StringUtil.offsetToLineColumn(document.text, editor.caretModel.offset) ?: return
        val position = LogicalPosition(lineCol.line, lineCol.column)
        // TODO this may be duplicated with caret events some times
        //      but without this there are cases no caret events but document changed events
        //      maybe some debounce
        file(file).updateCaret(editor, position, forceUpdate)
    }
}