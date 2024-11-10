package lean4ij.project

import lean4ij.infoview.external.ExternalInfoViewService
import lean4ij.project.listeners.LeanFileCaretListener
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEventMulticasterEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.startup.ProjectActivity
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import com.intellij.openapi.module.Module
import lean4ij.lsp.LeanLanguageServerFactory
import lean4ij.util.OsUtil
import java.awt.event.FocusEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.text.LineColumn
import com.intellij.openapi.util.text.StringUtil
import lean4ij.util.LeanUtil


fun Module.addExcludeFolder(basePath: String) {
    ModuleRootModificationUtil.updateModel(this) { rootModule ->
        // check https://github.com/intellij-rust/intellij-rust/issues/1062<
        // TODO any way not use Sdk or make a Sdk for lean?
        rootModule.inheritSdk()
        val contentEntries = rootModule.contentEntries
        contentEntries.singleOrNull()?.let { contentEntry ->
            // TODO excludeFolder seems still in Find
            contentEntry.addExcludePattern(".olean")
            contentEntry.addExcludePattern(".ilean")
            contentEntry.addExcludePattern(".c")
            // contentEntry.addSourceFolder()

            val lakePath = Path.of(basePath, ".lake")
            // skip normal project that is not a lean project and contains no .lake directory
            if (!lakePath.toFile().let { it.exists() && it.isDirectory }) {
                return@let
            }
            Files.walk(Path.of(basePath, ".lake"), 5)
                .filter { path -> path.isDirectory() }
                .forEach { path ->
                    // TODO these logger should change the level to trace
                    thisLogger().info("checking if $path should be excluded")
                    if (path.parent.name == ".lake" && path.name == "build" ) {
                        // TODO these logger should change the level to trace
                        // TODO extract this out and make a general class or something for normalizing all these url and path
                        thisLogger().info("adding $path to excludeFolder")
                        // must be of pattern "file://", the last replace is for fixing path in Windows...
                        val uri = path.toUri().toString().let {
                            var ret = it
                            if (OsUtil.isWindows()) {
                                // TODO switch back to Windows for testing this behavior
                                ret = it.replace("file:///", "file://")
                            }
                            ret
                        }

                        try {
                            contentEntry.addExcludeFolder(uri)
                            // TODO here thisLogger is Module
                            //      maybe it's better not using extension method
                            thisLogger().info("$path excluded")
                        } catch (ex: Exception) {
                            thisLogger().error("cannot exclude $uri", ex)
                        }
                    }
                }
        }
        rootModule.project.save()
    }

}
/**
 * see: [defining-project-level-listeners](https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html#defining-project-level-listeners)
 * this is copied from [ConnectDocumentToLanguageServerSetupParticipant](https://github.com/redhat-developer/lsp4ij/blob/cb04e064f93ec8c2bb22b216e54b6a7fb1c75496/src/main/java/com/redhat/devtools/lsp4ij/ConnectDocumentToLanguageServerSetupParticipant.java#L29)
 * no more from the above, now it implements [ProjectActivity]
 * ref: https://plugins.jetbrains.com/docs/intellij/plugin-components.html#project-and-application-close
 * and https://github.com/JetBrains/intellij-sdk-code-samples/blob/main/max_opened_projects/src/main/kotlin/org/intellij/sdk/maxOpenProjects/ProjectOpenStartupActivity.kt
 */
class LeanProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        setupModule(project)
        setupEditorFocusChangeEventListener(project)
        project.service<LeanProjectService>()
        project.service<LeanFileCaretListener>()
        project.service<ExternalInfoViewService>()
    }

    /**
     * ref: https://intellij-support.jetbrains.com/hc/en-us/community/posts/4578776718354-How-do-I-listen-for-editor-focus-events
     * TODO absolutely this requires some refactor
     *      this is for avoiding didOpen request that make the lean lsp server handling it and improve performance
     *      but it may have some false positive event though
     */
    private fun setupEditorFocusChangeEventListener(project: Project) {
        (EditorFactory.getInstance().eventMulticaster as? EditorEventMulticasterEx)?.let { ex ->
            ex.addFocusChangeListener(object: FocusChangeListener {
                override fun focusGained(editor: Editor) {
                    LeanLanguageServerFactory.isEnable.set(true)
                }

                override fun focusGained(editor: Editor, event: FocusEvent) {
                    LeanLanguageServerFactory.isEnable.set(true)
                }

                override fun focusLost(editor: Editor) {
                    LeanLanguageServerFactory.isEnable.set(false)
                }

                override fun focusLost(editor: Editor, event: FocusEvent) {
                    LeanLanguageServerFactory.isEnable.set(false)
                }
            }) {
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
            // ref: https://intellij-support.jetbrains.com/hc/en-us/community/posts/360006419280-DocumentListener-for-getting-what-line-was-changed
            // TODO extract DocumentListener
            ex.addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    val document = event.document
                    val file = FileDocumentManager.getInstance().getFile(document)?:return
                    if (!LeanUtil.isLeanFile(file)) {
                        return
                    }
                    try {
                        // TODO do some refactor here, extract similar code
                        val leanProjectService = project.service<LeanProjectService>()
                        val editor = EditorFactory.getInstance().getEditors(document).firstOrNull()?:return
                        val lineCol : LineColumn = StringUtil.offsetToLineColumn(document.text, event.offset) ?: return
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
            }) {
                // TODO Disposable
            }
        }
    }

    /**
     * Mostly this method is from
     * intellij-rust/src/main/kotlin/org/rust/ide/module/CargoConfigurationWizardStep.kt" 100 lines
     * for setting up module for ignoring .lake/build for index
     * check also intellij-rust/intellij
     */
    private fun setupModule(project: Project) {
        project.basePath?.let{ basePath ->
            project.projectFile?.let {
                thisLogger().info("current module is $it")
                // logger shows .idea/misc.xml
                val module = ModuleUtilCore.findModuleForFile(it, project)
                module?.addExcludeFolder(basePath)
            }
        }
    }

}