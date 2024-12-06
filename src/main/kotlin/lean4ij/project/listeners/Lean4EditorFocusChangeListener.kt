package lean4ij.project.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEventMulticasterEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import lean4ij.project.LeanProjectService
import java.awt.event.FocusEvent

/**
 * ref: https://intellij-support.jetbrains.com/hc/en-us/community/posts/4578776718354-How-do-I-listen-for-editor-focus-events
 */
class Lean4EditorFocusChangeListener : FocusChangeListener {
    override fun focusGained(editor: Editor) {
        val project = editor.project?:return
        project.service<LeanProjectService>().isEnable.set(true)
    }

    override fun focusGained(editor: Editor, event: FocusEvent) {
        val project = editor.project?:return
        project.service<LeanProjectService>().isEnable.set(true)
    }

    override fun focusLost(editor: Editor) {
        val project = editor.project?:return
        // avoiding set it to false for popup goto declaration requires
        // project.service<LeanProjectService>().isEnable.set(false)
    }

    override fun focusLost(editor: Editor, event: FocusEvent) {
        val project = editor.project?:return
        // avoiding set it to false for popup goto declaration requires
        // project.service<LeanProjectService>().isEnable.set(false)
        }

    fun register(editorEventMulticaster: EditorEventMulticasterEx) {
        // here switching the receiver from editorEventMulticaster to this is mainly for
        // the comment on disposers
        editorEventMulticaster.addFocusChangeListener(this) {
            // TODO add real Disposable, it's used for avoiding resource leak
            //      check com.intellij.codeInsight.daemon.impl.EditorTrackerImpl
            //      and the doc https://plugins.jetbrains.com/docs/intellij/disposers.html
            // TODO here it seems a leak, but it's only one at setup, maybe OK though
            //      from runIDE:
            //      2024-08-26 23:17:31,505 [ 968269] SEVERE - #c.i.o.u.ObjectTree - Memory leak detected: 'lean4ij.project.LeanProjectActivity$$Lambda$3778/0x0000000101753530@1f56b199' (class lean4ij.project.LeanProjectActivity$$Lambda$3778/0x0000000101753530) was registered in Disposer as a child of 'ROOT_DISPOSABLE' (class com.intellij.openapi.util.Disposer$2) but wasn't disposed.
            //      Register it with a proper parentDisposable or ensure that it's always disposed by direct Disposer.dispose call.
            //      See https://jetbrains.org/intellij/sdk/docs/basics/disposers.html for more details.
            //      The corresponding Disposer.register() stacktrace is shown as the cause:
            //      java.lang.RuntimeException: Memory leak detected: 'lean4ij.project.LeanProjectActivity$$Lambda$3778/0x0000000101753530@1f56b199' (class lean4ij.project.LeanProjectActivity$$Lambda$3778/0x0000000101753530) was registered in Disposer as a child of 'ROOT_DISPOSABLE' (class com.intellij.openapi.util.Disposer$2) but wasn't disposed.
            //      Register it with a proper parentDisposable or ensure that it's always disposed by direct Disposer.dispose call.
            //      See https://jetbrains.org/intellij/sdk/docs/basics/disposers.html for more details.
            //      The corresponding Disposer.register() stacktrace is shown as the cause:
            //      	at com.intellij.openapi.util.ObjectNode.assertNoChildren(ObjectNode.java:45)
            //      	at com.intellij.openapi.util.ObjectTree.assertIsEmpty(ObjectTree.java:219)
            //      	at com.intellij.openapi.util.Disposer.assertIsEmpty(Disposer.java:223)
            //      	at com.intellij.openapi.util.Disposer.assertIsEmpty(Disposer.java:217)
            //      	at com.intellij.openapi.application.impl.ApplicationImpl.disposeContainer(ApplicationImpl.java:197)
            //      	at com.intellij.openapi.application.impl.ApplicationImpl.destructApplication(ApplicationImpl.java:620)
            //      	at com.intellij.openapi.application.impl.ApplicationImpl.doExit(ApplicationImpl.java:551)
            //      	at com.intellij.openapi.application.impl.ApplicationImpl.exit(ApplicationImpl.java:536)
            //      	at com.intellij.openapi.application.impl.ApplicationImpl.exit(ApplicationImpl.java:525)
            //      	at com.intellij.openapi.application.ex.ApplicationEx.exit(ApplicationEx.java:78)
            //      	at com.intellij.openapi.wm.impl.CloseProjectWindowHelper.quitApp(CloseProjectWindowHelper.kt:67)
            //      	at com.intellij.openapi.wm.impl.CloseProjectWindowHelper.windowClosing(CloseProjectWindowHelper.kt:47)
            //      	at com.intellij.openapi.wm.impl.ProjectFrameHelper.windowClosing(ProjectFrameHelper.kt:434)
            //      	at com.intellij.openapi.wm.impl.WindowCloseListener.windowClosing(ProjectFrameHelper.kt:454)
            //      	at java.desktop/java.awt.AWTEventMulticaster.windowClosing(AWTEventMulticaster.java:357)
            //      	at java.desktop/java.awt.Window.processWindowEvent(Window.java:2114)
            //      	at java.desktop/javax.swing.JFrame.processWindowEvent(JFrame.java:298)
            //      	at java.desktop/java.awt.Window.processEvent(Window.java:2073)
            //      	at java.desktop/java.awt.Component.dispatchEventImpl(Component.java:5027)
            //      	at java.desktop/java.awt.Container.dispatchEventImpl(Container.java:2324)
            //      	at java.desktop/java.awt.Window.dispatchEventImpl(Window.java:2809)
            //      	at java.desktop/java.awt.Component.dispatchEvent(Component.java:4855)
            //      	at java.desktop/java.awt.EventQueue.dispatchEventImpl(EventQueue.java:794)
            //      	at java.desktop/java.awt.EventQueue$3.run(EventQueue.java:739)
            //      	at java.desktop/java.awt.EventQueue$3.run(EventQueue.java:733)
            //      	at java.base/java.security.AccessController.doPrivileged(AccessController.java:399)
            //      	at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:86)
            //      	at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:97)
            //      	at java.desktop/java.awt.EventQueue$4.run(EventQueue.java:766)
            //      	at java.desktop/java.awt.EventQueue$4.run(EventQueue.java:764)
            //      	at java.base/java.security.AccessController.doPrivileged(AccessController.java:399)
            //      	at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:86)
            //      	at java.desktop/java.awt.EventQueue.dispatchEvent(EventQueue.java:763)
            //      	at com.intellij.ide.IdeEventQueue.defaultDispatchEvent(IdeEventQueue.kt:698)
            //      	at com.intellij.ide.IdeEventQueue._dispatchEvent$lambda$12(IdeEventQueue.kt:593)
            //      	at com.intellij.openapi.application.impl.RwLockHolder.runWithoutImplicitRead(RwLockHolder.kt:105)
            //      	at com.intellij.ide.IdeEventQueue._dispatchEvent(IdeEventQueue.kt:593)
            //      	at com.intellij.ide.IdeEventQueue.access$_dispatchEvent(IdeEventQueue.kt:77)
            //      	at com.intellij.ide.IdeEventQueue$dispatchEvent$processEventRunnable$1$1$1.compute(IdeEventQueue.kt:362)
            //      	at com.intellij.ide.IdeEventQueue$dispatchEvent$processEventRunnable$1$1$1.compute(IdeEventQueue.kt:361)
            //      	at com.intellij.openapi.progress.impl.CoreProgressManager.computePrioritized(CoreProgressManager.java:843)
            //      	at com.intellij.ide.IdeEventQueue$dispatchEvent$processEventRunnable$1$1.invoke(IdeEventQueue.kt:361)
            //      	at com.intellij.ide.IdeEventQueue$dispatchEvent$processEventRunnable$1$1.invoke(IdeEventQueue.kt:356)
            //      	at com.intellij.ide.IdeEventQueueKt.performActivity$lambda$1(IdeEventQueue.kt:1021)
            //      	at com.intellij.openapi.application.TransactionGuardImpl.performActivity(TransactionGuardImpl.java:114)
            //      	at com.intellij.ide.IdeEventQueueKt.performActivity(IdeEventQueue.kt:1021)
            //      	at com.intellij.ide.IdeEventQueue.dispatchEvent$lambda$7(IdeEventQueue.kt:356)
            //      	at com.intellij.openapi.application.impl.RwLockHolder.runIntendedWriteActionOnCurrentThread(RwLockHolder.kt:209)
            //      	at com.intellij.openapi.application.impl.ApplicationImpl.runIntendedWriteActionOnCurrentThread(ApplicationImpl.java:830)
            //      	at com.intellij.ide.IdeEventQueue.dispatchEvent(IdeEventQueue.kt:398)
            //      	at java.desktop/java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:207)
            //      	at java.desktop/java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:128)
            //      	at java.desktop/java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:117)
            //      	at java.desktop/java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:113)
            //      	at java.desktop/java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:105)
            //      	at java.desktop/java.awt.EventDispatchThread.run(EventDispatchThread.java:92)
            //      Caused by: java.lang.Throwable
            //      	at com.intellij.openapi.util.ObjectNode.<init>(ObjectNode.java:24)
            //      	at com.intellij.openapi.util.ObjectNode.findOrCreateChildNode(ObjectNode.java:140)
            //      	at com.intellij.openapi.util.ObjectTree.register(ObjectTree.java:52)
            //      	at com.intellij.openapi.util.Disposer.register(Disposer.java:156)
            //      	at com.intellij.util.containers.DisposableWrapperList.createDisposableWrapper(DisposableWrapperList.java:246)
            //      	at com.intellij.util.containers.DisposableWrapperList.add(DisposableWrapperList.java:62)
            //      	at com.intellij.util.EventDispatcher.addListener(EventDispatcher.java:171)
            //      	at com.intellij.openapi.editor.impl.event.EditorEventMulticasterImpl.addFocusChangeListener(EditorEventMulticasterImpl.java:231)
            //      	at lean4ij.project.LeanProjectActivity.setupEditorFocusChangeEventListener(LeanProjectActivity.kt:91)
            //      	at lean4ij.project.LeanProjectActivity.execute(LeanProjectActivity.kt:77)
            //      	at com.intellij.ide.startup.impl.StartupManagerImplKt$launchActivity$1.invokeSuspend(StartupManagerImpl.kt:473)
            //      	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
            //      	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:108)
            //      	at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:584)
            //      	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:793)
            //      	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:697)
            //      	at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:684)
        }
    }
}