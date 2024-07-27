// package com.github.onriv.ijpluginlean.listeners
//
// import com.github.onriv.ijpluginlean.lsp.FileProgress
// import com.github.onriv.ijpluginlean.lsp.LeanLspServerManager
// import com.github.onriv.ijpluginlean.lsp.data.Position
// import com.github.onriv.ijpluginlean.services.CursorLocation
// import com.github.onriv.ijpluginlean.services.ExternalInfoViewService
// import com.github.onriv.ijpluginlean.services.Range
// // import com.github.onriv.ijpluginlean.services.CursorLocation
// // import com.github.onriv.ijpluginlean.services.ExternalInfoViewService
// // import com.github.onriv.ijpluginlean.services.Range
// import com.github.onriv.ijpluginlean.toolWindow.LeanInfoViewWindowFactory
// import com.intellij.openapi.components.service
// import com.intellij.openapi.diagnostic.thisLogger
// import com.intellij.openapi.editor.Caret
// import com.intellij.openapi.editor.Editor
// import com.intellij.openapi.editor.event.CaretEvent
// import com.intellij.openapi.editor.event.CaretListener
// import com.intellij.openapi.fileEditor.FileDocumentManager
// import com.intellij.openapi.fileEditor.FileEditorManager
// import com.intellij.openapi.progress.runBackgroundableTask
// import com.intellij.openapi.project.Project
// import com.intellij.openapi.vfs.VirtualFile
// import com.intellij.platform.lsp.api.Lsp4jClient
// import kotlinx.coroutines.*
// import kotlinx.coroutines.CoroutineScope
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.withContext
// import java.nio.charset.StandardCharsets
// import java.util.concurrent.ConcurrentHashMap
//
// class EditorCaretListener(val project: Project) : CaretListener {
//
//     companion object {
//
//         private val listeners: ConcurrentHashMap<Editor, EditorCaretListener> = ConcurrentHashMap();
//
//         @Synchronized
//         fun register(project: Project) {
//             // TODO what's the different with Editor and FileEditor?
//             // TODO real log
//             val editor: Editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
//             listeners.computeIfAbsent(editor) {
//                 val caretModel = editor.caretModel
//                 val listener = EditorCaretListener(project)
//                 caretModel.addCaretListener(listener)
//                 listener
//             }
//         }
//     }
//
//     private val infoView = project.service<ExternalInfoViewService>()
//
//
//     private fun shouldUpdateGoal(file: VirtualFile, caret: Caret) : Boolean {
//         if (!file.path.endsWith(".lean")) {
//             return false
//         }
//
//         // TODO more kotlin style to do this, using ?. etc...
//         val fileProgress = FileProgress.getFileProgress(LeanLspServerManager.tryFixWinUrl(file.url))
//         if (fileProgress == null || fileProgress.isProcessing()) {
//             thisLogger().debug("${file.path} is still processing, skip updating goal")
//             return false
//         }
//
//         // TODO getting text seems to be a performance issue?
//         // TODO is it instant data or from file or from memory?
//         // TODO not fix encoding, handle bom?
//         // TODO getting content is not instant... hence it's comment out
// //        val content = String(file.contentsToByteArray(), StandardCharsets.UTF_8)
// //        val lines = content.split(System.lineSeparator())
// //        if (lines.size <= caret.logicalPosition.line) {
// //            // TODO check why this happen
// //            thisLogger().debug("${file.path} has $content, but the caret line exceeds it with line ${caret.logicalPosition.line}")
// //            return false
// //        }
// //        val lineContent = lines[caret.logicalPosition.line]
// ////        val document = FileDocumentManager.getInstance().getDocument(file)
// ////        if (document == null) {
// ////            thisLogger().debug("${file.path} has no document in FileDocumentManager.")
// ////            return false
// ////        }
// ////        // TODO getting text seems to be a performance issue?
// ////        val lineContent = document.text.split("\n")[caret.logicalPosition.line]
// //        if (lineContent.startsWith("import ")) {
// //            thisLogger().debug("${file.path} with caret at import line, skip updating goal")
// //            return false
// //        }
// //        if (lineContent.startsWith("open ")) {
// //            thisLogger().debug("${file.path} with caret at open line, skip updating goal")
// //            return false
// //        }
//         return true
//     }
//
//     private val scope = CoroutineScope(Dispatchers.Main)
//
//     override fun caretPositionChanged(event: CaretEvent) {
//         val editor = event.editor
//         val file = editor.virtualFile
//         // TODO not really understand why here it can be null
//         if (event.caret == null) {
//             thisLogger().debug("${file.path} moved with caret null.")
// //                return@launch
//         }
//         thisLogger().debug("${file.path} moved to ${event.caret}")
//         if (event.editor.project == null) {
//             thisLogger().debug("${file.path} moved with project null.")
//         }
//         // TODO handle null
//         val project = event.editor.project!!
// //                return@launch
//         // or, withContext(Dispatchers.IO) ?
//         // TODO check kotlin's coroutine document
//         //      it's no way start coroutine from normal thread except runBlocking
//         //      see: https://stackoverflow.com/questions/76187252/
//         //      TODO but!, it seems the following way does work:
//         // class CoroutineService : Service() {
//         //     private val scope = CoroutineScope(Dispatchers.IO)
//         //
//         //     private val flow = MutableSharedFlow<String>(extraBufferCapacity = 64)
//         //
//         //     override fun onCreate() {
//         //         super.onCreate()
//         //         // collect data emitted by the Flow
//         //         flow.onEach {
//         //             // Handle data
//         //         }.launchIn(scope)
//         //     }
//         //
//         //     override fun onStartCommand(@Nullable intent: Intent?, flags: Int, startId: Int): Int {
//         //         scope.launch {
//         //             // retrieve data from Intent and send it to Flow
//         //             val messageFromIntent = intent?.let { it.extras?.getString("KEY_MESSAGE")} ?: ""
//         //             flow.emit(messageFromIntent)
//         //         }
//         //         return START_STICKY
//         //     }
//         //
//         //     override fun onBind(intent: Intent?): IBinder?  = null
//         //
//         //     override fun onDestroy() {
//         //         scope.cancel() // cancel CoroutineScope and all launched coroutines
//         //     }
//         // }
//         // check https://stackoverflow.com/questions/68279594/how-to-use-mutablesharedflow-in-android-service
//         runBackgroundableTask("Updating goal", project) {
//             val caret = event.caret!!
//             if (!shouldUpdateGoal(file, caret)) {
//                 return@runBackgroundableTask
//             }
//             try {
//                 // val plainGoal = LeanLspServerManager.getInstance(project).plainGoal(file, caret)
//                 // val plainTermGoal = LeanLspServerManager.getInstance(project).plainTermGoal(file, caret)
//                 // // TODO in fact, plain goal not works any more in current infoview window
//                 // // LeanInfoViewWindowFactory.updateGoal(project, file, caret, plainGoal, plainTermGoal)
//                 // //  Error response from server: org.eclipse.lsp4j.jsonrpc.ResponseErrorException: Outdated RPC sessios
//                 // val interactiveGoal = LeanLspServerManager.getInstance(project).getInteractiveGoals(file, event.caret!!)
//                 // if (interactiveGoal != null) {
//                 //     LeanInfoViewWindowFactory.updateInteractiveGoal(project, file, caret, interactiveGoal)
//                 // }
//             } catch (e: Exception) {
//                 // TODO handle it
//                 e.printStackTrace()
//             }
//
//             try {
//                 // TODO DIY
//                 val logicalPosition = event.caret!!.logicalPosition
//                 val position = Position(line=logicalPosition.line, character = logicalPosition.column)
//                 infoView.changedCursorLocation(
//                     CursorLocation(
//                     uri = LeanLspServerManager.tryFixWinUrl(file.toString()),
//                     range = Range(start = position, end=position)
//                 )
//                 )
//             } catch (e: Exception) {
//                 // TODO handle it
//                 e.printStackTrace()
//             }
//
//         }
//
//
//
//
//     }
// }