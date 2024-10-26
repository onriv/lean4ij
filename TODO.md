
# TODO

- [x] file progressing seems block UI thread in some cases 
  - solved by highlight on ranges rather than lines
- [x] skip index `.lake/build`
  - [x] this can be manually done by right-clicking the folder and marking it as exclude
  - [x] automatically exclude, check [this](https://youtrack.jetbrains.com/issue/IDEA-194725/Specify-IntelliJ-exclude-directories-in-build.gradle), or [this](https://youtrack.jetbrains.com/issue/IJPL-8363/Ability-to-have-default-Excluded-Folders-not-per-project), or [this](https://youtrack.jetbrains.com/issue/WEB-11419).
    some plugins have customized logic for it like intellij-rust or intellij-arend
- [ ] infoview toolwindow in swing
  - [x] show goals
  - [x] show term goal
  - [x] show message
  - [x] interactive message
  - [x] show all messages (all messages currently is skipped for not sure when it's trigger)
  - [x] interactive all messages
  - [x] popup
  - [ ] pop up style, fonts, clickable links, etc
  - [x] color
  - [x] make the editor singleton
- [ ] mathlib4 seems always failed starting the language server
  this is because elan download lake while starting lsp, not fixed yet
- [x] infoview toolwindow in jcef
- [ ] project create/setup or configuration
- [ ] distinguish source in .lake as library rather than source
- [x] avoid file progressing in search window (it should be caused by didOpen, etc.) solved by only enable lsp at focusing editor
- [ ] setting dialog
- [x] theme and color
- [x] find in files will send a didOpen request and make fileProgress, it may hurt the performance.
  currently a fix for this is disabling lsp while lost focus for the editor
- [ ] elan/lake, project create, setup etc
- [ ] run and build (debug cannot be supported, although arend has this)
- [ ] some more logs with different levels
- [ ] refactor the frontend impl (currently it's written as for feasibility test)
- [x] all messages in the external infoview failed (via caching server notification now)
- [ ] check why sometimes lsp requires multiple start
- [x] all message should be interactive (check lean-infoview/src/infoview/info.tsx)
      fixed via passing `hasWidgets` when start lsp
- [ ] jcef infoview style adjust
- [ ] unify jcef/browser/external/vscode infoview font name...
- [ ] jcef infoview popup link should be opened in external web browser
- [ ] setting page
  - [x] added a setting page
  - [ ] add some other settings
  - [ ] lsp autocompletion disable setting
  - [ ] setting for getAllMessages
- [ ] line comment and block comment
- [ ] weird, is it just me or any other reason making only word autocompletion not working? In comment, it works but in normal pos it does not. It seems it's superseded by 
  some semantic autocompletion. --- yeah it's because semantic autocompletion is too slow. Can it be done in two steps? first show alphabet autocompletion and then add more semantic 
  autocompletion 
- [ ] after bump lsp4ij to 0.7.0, make autocompletion configurable
- [ ] quick fix for field missing
- [x] color for error message
- [ ] code completion seems slow and requires manually press ctrl+space
- [ ] TODO is not highlight
– [ ] Autocomplete is slow... like in vscode. Maybe disable it or improve the lean server end
– [x] all messages logic is still wrong maybe it's flushed by new diagnostics
– [x] two cases still exists for all messages: this should already be fixed
  1. it's not shown
  2. it's outdated
– [x] some snippets to things like `\<>`
- [ ] for some snippets maybe it's better to add a space, like `\to`, now for triggering it, it requires a space. But most case it will continue with a space.
  - But not sure for the design, some absolutely don't want a auto created space
– [ ] TODO weird brackets does not complete
  - [here](https://github.com/intellij-rust/intellij-rust/issues/1076) maybe related
– [ ] maybe it's still better define some lang-like feature using parser/lexer, although it cannot be full parsed, but for the level like textmate it should be OK
– [ ] is it possible do something like pygments/ctags/gtags completion?
– [ ] option to skip library or backend files
– [ ] error seems quite delay vanish... it shows errors event it has been fixed.
- [ ] the internal infoview some case also delay (especially using ideavim one does not move caret)
– [ ] comment auto comment like /-- trigger block comment
– [ ] bock/line comment command
– [ ] impl simp? which replace the code
- [ ] settings for getAllMessages, both internal/external infoview
- [ ] maybe it's just me with my idea settings, the gap between first column and line number is a little width
- [ ] autogenerate missing fields
- [ ] internal infoview when expanding all messages it seems jumping
- [ ] check if live templates can dynamically define or not, in this way we can control if suffix space add automatically or not
- [x] internal infoview will automatically scroll to the end, kind of disturbing
- [ ] it's very hard to type the trigger for the snippet out like \a for α if there is some space following it. Some possible way is like type \ablablala<space> and then delete the unneeded part
      should this be considered?
- [ ] maybe internal infoview should be soft wrap. If the text is too long, currently only making the toolwindow wider we can see it
- [ ] internal infoview update is not instant (It may require some update of cursor)
- [ ] if the project contains some failed to build event, the internal infoview totally failed to update

# Some exceptions that should be tracked

```java
java.lang.NullPointerException: getBackgroundColor(...) must not be null
    at lean4ij.infoview.InfoviewMouseMotionListener$mouseMoved$attr$1.getBackgroundColor(InfoviewMouseMotionListener.kt:95)
    at com.intellij.openapi.editor.impl.view.IterationState.setAttributes(IterationState.java:571)
    at com.intellij.openapi.editor.impl.view.IterationState.reinit(IterationState.java:465)
    at com.intellij.openapi.editor.impl.view.IterationState.advance(IterationState.java:211)
    at com.intellij.openapi.editor.impl.view.EditorPainter$Session.paintLineFragments(EditorPainter.java:1503)
    at com.intellij.openapi.editor.impl.view.EditorPainter$Session.paintBackground(EditorPainter.java:369)
    at com.intellij.openapi.editor.impl.view.EditorPainter$Session.paint(EditorPainter.java:194)
    at com.intellij.openapi.editor.impl.view.EditorPainter.paint(EditorPainter.java:86)
    at com.intellij.openapi.editor.impl.view.EditorView.paint(EditorView.java:283)
    at com.intellij.openapi.editor.impl.EditorImpl.lambda$paint$47(EditorImpl.java:2070)
    at com.intellij.openapi.application.impl.AnyThreadWriteThreadingSupport.runReadAction$lambda$2(AnyThreadWriteThreadingSupport.kt:217)
    at com.intellij.openapi.application.impl.AnyThreadWriteThreadingSupport.runReadAction(AnyThreadWriteThreadingSupport.kt:228)
    at com.intellij.openapi.application.impl.AnyThreadWriteThreadingSupport.runReadAction(AnyThreadWriteThreadingSupport.kt:217)
    at com.intellij.openapi.application.impl.ApplicationImpl.runReadAction(ApplicationImpl.java:847)
    at com.intellij.openapi.editor.impl.EditorImpl.paint(EditorImpl.java:2060)
    at com.intellij.openapi.editor.impl.EditorComponentImpl.paintComponent(EditorComponentImpl.java:283)
    at java.desktop/javax.swing.JComponent.paint(JComponent.java:1124)
    at com.intellij.openapi.editor.impl.EditorComponentImpl.paint(EditorComponentImpl.java:157)
    at java.desktop/javax.swing.JComponent.paintChildren(JComponent.java:964)
    at java.desktop/javax.swing.JComponent.paint(JComponent.java:1133)
    at java.desktop/javax.swing.JViewport.paint(JViewport.java:736)
    at com.intellij.ui.components.JBViewport.paint(JBViewport.java:240)
    at java.desktop/javax.swing.JComponent.paintChildren(JComponent.java:964)
    at com.intellij.ui.components.JBScrollPane.paintChildren(JBScrollPane.java:242)
    at java.desktop/javax.swing.JComponent.paint(JComponent.java:1133)
    at com.intellij.ui.components.JBScrollPane.paint(JBScrollPane.java:230)
    at java.desktop/javax.swing.JComponent.paintChildren(JComponent.java:964)
    at java.desktop/javax.swing.JComponent.paint(JComponent.java:1133)
    at java.desktop/javax.swing.JLayeredPane.paint(JLayeredPane.java:586)
    at java.desktop/javax.swing.JComponent.paintChildren(JComponent.java:964)
    at java.desktop/javax.swing.JComponent.paint(JComponent.java:1133)
    at java.desktop/javax.swing.JComponent.paintChildren(JComponent.java:964)
    at java.desktop/javax.swing.JComponent.paint(JComponent.java:1133)
    at java.desktop/javax.swing.JComponent.paintChildren(JComponent.java:964)
    at java.desktop/javax.swing.JComponent.paint(JComponent.java:1133)
    at java.desktop/javax.swing.JComponent.paintChildren(JComponent.java:964)
    at java.desktop/javax.swing.JComponent.paint(JComponent.java:1133)
    at java.desktop/javax.swing.JComponent.paintChildren(JComponent.java:964)
    at java.desktop/javax.swing.JComponent.paint(JComponent.java:1133)
    at java.desktop/javax.swing.JComponent.paintToOffscreen(JComponent.java:5319)
    at java.desktop/javax.swing.RepaintManager$PaintManager.paintDoubleBufferedImpl(RepaintManager.java:1680)
    at java.desktop/javax.swing.RepaintManager$PaintManager.paintDoubleBuffered(RepaintManager.java:1655)
    at java.desktop/javax.swing.RepaintManager$PaintManager.paint(RepaintManager.java:1592)
    at java.desktop/javax.swing.BufferStrategyPaintManager.paint(BufferStrategyPaintManager.java:281)
    at java.desktop/javax.swing.RepaintManager.paint(RepaintManager.java:1352)
    at java.desktop/javax.swing.JComponent._paintImmediately(JComponent.java:5267)
    at java.desktop/javax.swing.JComponent.paintImmediately(JComponent.java:5077)
    at java.desktop/javax.swing.RepaintManager$4.run(RepaintManager.java:887)
    at java.desktop/javax.swing.RepaintManager$4.run(RepaintManager.java:870)
    at java.base/java.security.AccessController.doPrivileged(AccessController.java:400)
    at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:87)
    at java.desktop/javax.swing.RepaintManager.paintDirtyRegions(RepaintManager.java:870)
    at java.desktop/javax.swing.RepaintManager.paintDirtyRegions(RepaintManager.java:843)
    at java.desktop/javax.swing.RepaintManager.prePaintDirtyRegions(RepaintManager.java:789)
    at java.desktop/javax.swing.RepaintManager$ProcessingRunnable.run(RepaintManager.java:1921)
    at java.desktop/java.awt.event.InvocationEvent.dispatch(InvocationEvent.java:318)
    at java.desktop/java.awt.EventQueue.dispatchEventImpl(EventQueue.java:781)
    at java.desktop/java.awt.EventQueue$4.run(EventQueue.java:728)
    at java.desktop/java.awt.EventQueue$4.run(EventQueue.java:722)
    at java.base/java.security.AccessController.doPrivileged(AccessController.java:400)
    at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:87)
    at java.desktop/java.awt.EventQueue.dispatchEvent(EventQueue.java:750)
    at com.intellij.ide.IdeEventQueue.defaultDispatchEvent(IdeEventQueue.kt:696)
    at com.intellij.ide.IdeEventQueue._dispatchEvent$lambda$16(IdeEventQueue.kt:590)
    at com.intellij.openapi.application.impl.AnyThreadWriteThreadingSupport.runWithoutImplicitRead(AnyThreadWriteThreadingSupport.kt:117)
    at com.intellij.ide.IdeEventQueue._dispatchEvent(IdeEventQueue.kt:590)
    at com.intellij.ide.IdeEventQueue.access$_dispatchEvent(IdeEventQueue.kt:73)
    at com.intellij.ide.IdeEventQueue$dispatchEvent$processEventRunnable$1$1$1$1.compute(IdeEventQueue.kt:357)
    at com.intellij.ide.IdeEventQueue$dispatchEvent$processEventRunnable$1$1$1$1.compute(IdeEventQueue.kt:356)
    at com.intellij.openapi.progress.impl.CoreProgressManager.computePrioritized(CoreProgressManager.java:843)
    at com.intellij.ide.IdeEventQueue$dispatchEvent$processEventRunnable$1$1$1.invoke(IdeEventQueue.kt:356)
    at com.intellij.ide.IdeEventQueue$dispatchEvent$processEventRunnable$1$1$1.invoke(IdeEventQueue.kt:351)
    at com.intellij.ide.IdeEventQueueKt$performActivity$runnableWithWIL$1.invoke$lambda$0(IdeEventQueue.kt:1035)
    at com.intellij.openapi.application.WriteIntentReadAction.lambda$run$0(WriteIntentReadAction.java:24)
    at com.intellij.openapi.application.impl.AnyThreadWriteThreadingSupport.runWriteIntentReadAction(AnyThreadWriteThreadingSupport.kt:84)
    at com.intellij.openapi.application.impl.ApplicationImpl.runWriteIntentReadAction(ApplicationImpl.java:910)
    at com.intellij.openapi.application.WriteIntentReadAction.compute(WriteIntentReadAction.java:55)
    at com.intellij.openapi.application.WriteIntentReadAction.run(WriteIntentReadAction.java:23)
    at com.intellij.ide.IdeEventQueueKt$performActivity$runnableWithWIL$1.invoke(IdeEventQueue.kt:1035)
    at com.intellij.ide.IdeEventQueueKt$performActivity$runnableWithWIL$1.invoke(IdeEventQueue.kt:1035)
    at com.intellij.ide.IdeEventQueueKt.performActivity$lambda$1(IdeEventQueue.kt:1036)
    at com.intellij.openapi.application.TransactionGuardImpl.performActivity(TransactionGuardImpl.java:106)
    at com.intellij.ide.IdeEventQueueKt.performActivity(IdeEventQueue.kt:1036)
    at com.intellij.ide.IdeEventQueue.dispatchEvent$lambda$10(IdeEventQueue.kt:351)
    at com.intellij.ide.IdeEventQueue.dispatchEvent(IdeEventQueue.kt:389)
    at java.desktop/java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:207)
    at java.desktop/java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:128)
    at java.desktop/java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:117)
    at java.desktop/java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:113)
    at java.desktop/java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:105)
    at java.desktop/java.awt.EventDispatchThread.run(EventDispatchThread.java:92)
```

and:

```java
java.lang.Throwable: no build for Proofs.lean
    at com.intellij.openapi.diagnostic.Logger.error(Logger.java:376)
    at lean4ij.project.BuildWindowService$1$1.emit(BuildWindowService.kt:79)
    at lean4ij.project.BuildWindowService$1$1.emit(BuildWindowService.kt:56)
    at kotlinx.coroutines.flow.SharedFlowImpl.collect$suspendImpl(SharedFlow.kt:392)
    at kotlinx.coroutines.flow.SharedFlowImpl$collect$1.invokeSuspend(SharedFlow.kt)
    at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
    at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:104)
    at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:608)
    at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:873)
    at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:763)
    at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:750)
```

and

```java
Error while consuming LSP 'completionItem/resolve' request

java.util.concurrent.ExecutionException: org.eclipse.lsp4j.jsonrpc.ResponseErrorException: Cannot process request to closed file ''
    at java.base/java.util.concurrent.CompletableFuture.reportGet(CompletableFuture.java:396)
    at java.base/java.util.concurrent.CompletableFuture.get(CompletableFuture.java:2096)
    at com.redhat.devtools.lsp4ij.internal.CompletableFutures.waitUntilDone(CompletableFutures.java:119)
    at com.redhat.devtools.lsp4ij.client.features.LSPCompletionProposal.getResolvedCompletionItem(LSPCompletionProposal.java:445)
    at com.redhat.devtools.lsp4ij.client.features.LSPCompletionProposal.getExpensiveRenderer(LSPCompletionProposal.java:229)
    at com.intellij.codeInsight.lookup.LookupElementDecorator.getExpensiveRenderer(LookupElementDecorator.java:101)
    at com.intellij.codeInsight.lookup.impl.LookupCellRenderer.updateItemPresentation(LookupCellRenderer.java:633)
    at com.intellij.codeInsight.lookup.impl.LookupImpl.scheduleItemUpdate(LookupImpl.java:302)
    at com.redhat.devtools.lsp4ij.features.completion.LSPCompletionContributor$LSPLookupManagerListener$1.currentItemChanged(LSPCompletionContributor.java:205)
    at com.intellij.codeInsight.lookup.impl.LookupImpl.fireCurrentItemChanged(LookupImpl.java:1032)
    at com.intellij.codeInsight.lookup.impl.LookupImpl.refreshUi(LookupImpl.java:1240)
    at com.intellij.codeInsight.completion.CompletionProgressIndicator.hideAutopopupIfMeaningless(CompletionProgressIndicator.java:732)
    at com.intellij.codeInsight.completion.CompletionProgressIndicator.updateLookup(CompletionProgressIndicator.java:456)
    at com.intellij.codeInsight.completion.CompletionProgressIndicator.lambda$stop$6(CompletionProgressIndicator.java:719)
    at com.intellij.util.concurrency.ContextRunnable.run(ContextRunnable.java:27)
    at com.intellij.openapi.application.TransactionGuardImpl.runWithWritingAllowed(TransactionGuardImpl.java:229)
    at com.intellij.openapi.application.TransactionGuardImpl.access$100(TransactionGuardImpl.java:22)
    at com.intellij.openapi.application.TransactionGuardImpl$1.run(TransactionGuardImpl.java:191)
    at com.intellij.openapi.application.impl.AnyThreadWriteThreadingSupport.runIntendedWriteActionOnCurrentThread$lambda$1(AnyThreadWriteThreadingSupport.kt:184)
    at com.intellij.openapi.application.impl.AnyThreadWriteThreadingSupport.runWriteIntentReadAction(AnyThreadWriteThreadingSupport.kt:84)
    at com.intellij.openapi.application.impl.AnyThreadWriteThreadingSupport.runIntendedWriteActionOnCurrentThread(AnyThreadWriteThreadingSupport.kt:183)
    at com.intellij.openapi.application.impl.ApplicationImpl.runIntendedWriteActionOnCurrentThread(ApplicationImpl.java:836)
    at com.intellij.openapi.application.impl.ApplicationImpl$2.run(ApplicationImpl.java:424)
    at com.intellij.openapi.application.impl.AnyThreadWriteThreadingSupport.runWithImplicitRead(AnyThreadWriteThreadingSupport.kt:122)
    at com.intellij.openapi.application.impl.ApplicationImpl.runWithImplicitRead(ApplicationImpl.java:1162)
    at com.intellij.openapi.application.impl.FlushQueue.doRun(FlushQueue.java:78)
    at com.intellij.openapi.application.impl.FlushQueue.runNextEvent(FlushQueue.java:119)
    at com.intellij.openapi.application.impl.FlushQueue.flushNow(FlushQueue.java:41)
    at java.desktop/java.awt.event.InvocationEvent.dispatch(InvocationEvent.java:318)
    at java.desktop/java.awt.EventQueue.dispatchEventImpl(EventQueue.java:781)
    at java.desktop/java.awt.EventQueue$4.run(EventQueue.java:728)
    at java.desktop/java.awt.EventQueue$4.run(EventQueue.java:722)
    at java.base/java.security.AccessController.doPrivileged(AccessController.java:400)
    at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:87)
    at java.desktop/java.awt.EventQueue.dispatchEvent(EventQueue.java:750)
    at com.intellij.ide.IdeEventQueue.defaultDispatchEvent(IdeEventQueue.kt:696)
    at com.intellij.ide.IdeEventQueue._dispatchEvent$lambda$16(IdeEventQueue.kt:590)
    at com.intellij.openapi.application.impl.AnyThreadWriteThreadingSupport.runWithoutImplicitRead(AnyThreadWriteThreadingSupport.kt:117)
    at com.intellij.ide.IdeEventQueue._dispatchEvent(IdeEventQueue.kt:590)
    at com.intellij.ide.IdeEventQueue.access$_dispatchEvent(IdeEventQueue.kt:73)
    at com.intellij.ide.IdeEventQueue$dispatchEvent$processEventRunnable$1$1$1$1.compute(IdeEventQueue.kt:357)
    at com.intellij.ide.IdeEventQueue$dispatchEvent$processEventRunnable$1$1$1$1.compute(IdeEventQueue.kt:356)
    at com.intellij.openapi.progress.impl.CoreProgressManager.computePrioritized(CoreProgressManager.java:843)
    at com.intellij.ide.IdeEventQueue$dispatchEvent$processEventRunnable$1$1$1.invoke(IdeEventQueue.kt:356)
    at com.intellij.ide.IdeEventQueue$dispatchEvent$processEventRunnable$1$1$1.invoke(IdeEventQueue.kt:351)
    at com.intellij.ide.IdeEventQueueKt$performActivity$runnableWithWIL$1.invoke$lambda$0(IdeEventQueue.kt:1035)
    at com.intellij.openapi.application.WriteIntentReadAction.lambda$run$0(WriteIntentReadAction.java:24)
    at com.intellij.openapi.application.impl.AnyThreadWriteThreadingSupport.runWriteIntentReadAction(AnyThreadWriteThreadingSupport.kt:84)
    at com.intellij.openapi.application.impl.ApplicationImpl.runWriteIntentReadAction(ApplicationImpl.java:910)
    at com.intellij.openapi.application.WriteIntentReadAction.compute(WriteIntentReadAction.java:55)
    at com.intellij.openapi.application.WriteIntentReadAction.run(WriteIntentReadAction.java:23)
    at com.intellij.ide.IdeEventQueueKt$performActivity$runnableWithWIL$1.invoke(IdeEventQueue.kt:1035)
    at com.intellij.ide.IdeEventQueueKt$performActivity$runnableWithWIL$1.invoke(IdeEventQueue.kt:1035)
    at com.intellij.ide.IdeEventQueueKt.performActivity$lambda$1(IdeEventQueue.kt:1036)
    at com.intellij.openapi.application.TransactionGuardImpl.performActivity(TransactionGuardImpl.java:106)
    at com.intellij.ide.IdeEventQueueKt.performActivity(IdeEventQueue.kt:1036)
    at com.intellij.ide.IdeEventQueue.dispatchEvent$lambda$10(IdeEventQueue.kt:351)
    at com.intellij.ide.IdeEventQueue.dispatchEvent(IdeEventQueue.kt:397)
    at java.desktop/java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:207)
    at java.desktop/java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:128)
    at java.desktop/java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:117)
    at java.desktop/java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:113)
    at java.desktop/java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:105)
    at java.desktop/java.awt.EventDispatchThread.run(EventDispatchThread.java:92)
Caused by: org.eclipse.lsp4j.jsonrpc.ResponseErrorException: Cannot process request to closed file ''
    at org.eclipse.lsp4j.jsonrpc.RemoteEndpoint.handleResponse(RemoteEndpoint.java:209)
    at org.eclipse.lsp4j.jsonrpc.RemoteEndpoint.consume(RemoteEndpoint.java:193)
    at com.redhat.devtools.lsp4ij.LanguageServerWrapper.lambda$start$2(LanguageServerWrapper.java:268)
    at java.base/java.util.concurrent.CompletableFuture$AsyncRun.run(CompletableFuture.java:1804)
    at java.base/java.util.concurrent.CompletableFuture$AsyncRun.exec(CompletableFuture.java:1796)
    at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:507)
    at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1491)
    at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:2073)
    at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:2035)
    at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:187)
```

# Maybe some improvements

it is not automatically indent:
```lean
structure Submonoid₁ (M : Type) [Monoid M] where<ENTER HERE DOES NOT INTENT>
  carrior : Set M
```