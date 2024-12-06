package lean4ij.project.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEventMulticasterEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.LineColumn
import com.intellij.openapi.util.text.StringUtil
import lean4ij.project.LeanProjectService
import lean4ij.util.LeanUtil

/**
 * ref: https://intellij-support.jetbrains.com/hc/en-us/community/posts/360006419280-DocumentListener-for-getting-what-line-was-changed
 */
class Lean4DocumentListener(private val project: Project) : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
        val document = event.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        if (!LeanUtil.isLeanFile(file)) {
            return
        }
        try {
            // TODO do some refactor here, extract similar code
            val leanProjectService = project.service<LeanProjectService>()
            val editor = EditorFactory.getInstance().getEditors(document).firstOrNull() ?: return
            val lineCol: LineColumn = StringUtil.offsetToLineColumn(document.text, event.offset) ?: return
            val position = LogicalPosition(lineCol.line, lineCol.column)
            // TODO this may be duplicated with caret events some times
            //      but without this there are cases no caret events but document changed events
            //      maybe some debounce
            leanProjectService.file(file).updateCaret(editor, position)
        } catch (ex: Exception) {
            // TODO the project.service above once threw:
            //     Caused by: com.intellij.openapi.progress.ProcessCanceledException: com.intellij.platform.instanceContainer.internal.ContainerDisposedException: Container 'ProjectImpl@518117404 services' was disposed
            // 	            at com.intellij.serviceContainer.ComponentManagerImpl.doGetService(ComponentManagerImpl.kt:717)
            // 	            at com.intellij.serviceContainer.ComponentManagerImpl.getService(ComponentManagerImpl.kt:690)
            // 	            at lean4ij.project.LeanProjectActivity$setupEditorFocusChangeEventListener$1$3.documentChanged(LeanProjectActivity.kt:257)
            // 	            at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
            // 	            at java.base/java.lang.reflect.Method.invoke(Method.java:580)
            // 	            at com.intellij.util.EventDispatcher.dispatchVoidMethod(EventDispatcher.java:119)
            // 	            at com.intellij.util.EventDispatcher.lambda$createMulticaster$1(EventDispatcher.java:84)
            // 	            at jdk.proxy2/jdk.proxy2.$Proxy104.documentChanged(Unknown Source)
            // 	            at com.intellij.openapi.editor.impl.DocumentImpl.lambda$changedUpdate$1(DocumentImpl.java:924)
            // 	            ... 96 more
        }
    }

    fun register(editorEventMulticaster: EditorEventMulticasterEx) {
        editorEventMulticaster.addDocumentListener(this) {
            // TODO Disposable
        }
    }
}