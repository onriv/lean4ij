# 2026-10-10 — "Access is allowed from EDT only" from `InfoViewWindowFactory.getLeanInfoview`

## Symptom (IDEA 2027.1 nightly, idea.log)

```
com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments: Access is allowed from Event Dispatch Thread (EDT) only; see https://jb.gg/ij-platform-threading for details
Current thread: Thread[#109,DefaultDispatcher-worker-52,5,main] ...
    at com.intellij.util.concurrency.ThreadingAssertions.softAssertEventDispatchThread(ThreadingAssertions.java:134)
    at com.intellij.openapi.wm.impl.ToolWindowImpl.createContentIfNeeded(ToolWindowImpl.kt:775)
    at com.intellij.openapi.wm.impl.ToolWindowImpl.getContentManager(ToolWindowImpl.kt:634)
    at lean4ij.infoview.InfoViewWindowFactory$Companion.getLeanInfoview(InfoViewWindowFactory.kt:46)
    at lean4ij.infoview.InfoViewWindowFactory$Companion.updateInteractiveGoal(InfoViewWindowFactory.kt:158)
    at lean4ij.project.LeanFile$updateInternalInfoview$1.invokeSuspend(LeanFile.kt:268)
```

## Root cause

`LeanFile.updateInternalInfoview` runs the goal/term-goal/diagnostics RPCs in `leanProjectService.scope.launch { }`
(default dispatcher) and then calls `InfoViewWindowFactory.updateInteractiveGoal` on that same background thread.
`updateInteractiveGoal` resolved the tool window via `getLeanInfoview`, which used `ToolWindow.getContentManager()`.
That getter lazily creates the tool window content (`ToolWindowImpl.createContentIfNeeded`) and therefore asserts EDT.
Older platforms did not assert here (or the content was already created by the time the first update arrived),
2027.1 reports it as a logged error on every infoview update.

## Fix

- `getLeanInfoview` now uses `ToolWindow.getContentManagerIfCreated()` (available since 2020.x, so fine for
  `sinceBuild=242`): it only reads the already created manager and is safe from any thread. If the tool window was
  never opened there is nothing to update, so `null` is the correct answer anyway. The cast was also softened to `as?`.
- `updateInteractiveGoal` resolves the tool window inside the existing `scope.launch(Dispatchers.EDT)` block instead of
  on the caller's thread, so all tool window access stays on EDT.

The other callers (`ToggleLeanInfoViewInternal.actionPerformed`, the "Internal infoview is not enable." branch in
`LeanFile.updateCaret`) are also covered by the `contentManagerIfCreated` change.
