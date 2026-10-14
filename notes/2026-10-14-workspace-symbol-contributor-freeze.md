# 2026-10-14 — short EDT freezes while typing in a non-Lean editor (IDEA 2027.1 nightly 271.494)

## Symptom

The IDE stutters (freezes of ~1 s) while typing in the Air plugin's chat/markdown editor inside a Lean project.
Dump: `threadDumps-freeze-popup-20261013-235049-IU-271.494/threadDump-20261014-111123-1791965483720.txt`.

## Stacks (abbreviated)

EDT waits for the write lock to start completion:

```
"AWT-EventQueue-0" TIMED_WAITING
	at com.intellij.openapi.progress.util.SuvorovProgress.dispatchEventsUntilComputationCompletes
	at com.intellij.platform.locking.impl.NestedLocksThreadingSupport.runWriteActionBlocking
	at com.intellij.openapi.application.WriteAction.compute
	at com.intellij.codeInsight.completion.CompletionInitializationUtil.createCompletionInitializationContext
	at com.intellij.codeInsight.completion.CodeCompletionHandlerBase.invokeCompletion
```

The read action it waits for is ours, sleeping in the debounce loop:

```
"DefaultDispatcher-worker-45" TIMED_WAITING
	at java.lang.Thread.sleep
	at lean4ij.language.WorkspaceSymbolsCache.getWorkspaceSymbolsTriggeredByDebounce(Lean4WorkspaceSymbolContributor.kt:201)
	at lean4ij.language.WorkspaceSymbolsCache.getWorkspaceSymbols(Lean4WorkspaceSymbolContributor.kt:232)
	at lean4ij.language.Lean4ChooseByNameContributorEx.processElementsWithName(Lean4WorkspaceSymbolContributor.kt:76)
	at org.intellij.plugins.markdown.lang.references.backtick.BacktickReference.resolveWithContributor
	at org.intellij.plugins.markdown.lang.references.backtick.BacktickReference.multiResolve
	at com.intellij.codeInsight.TargetElementUtilBase.findTargetElement
	at com.intellij.codeInsight.daemon.impl.IdentifierHighlightingComputer.computeRanges
	at com.intellij.openapi.application.rw.InternalReadAction.tryReadCancellable
```

## Root cause

1. `processElementsWithName` of a `gotoSymbolContributor` is not only invoked by the Goto Symbol popup. The
   Markdown plugin's `BacktickReference` resolves every `` `code` `` span through all contributors, and the
   identifier-highlighting pass does that on every caret move / edit, inside a cancellable read action.
2. Our `WorkspaceSymbolsCache.getWorkspaceSymbolsTriggeredByDebounce` debounced the request with a plain
   `Thread.sleep` loop (up to `workspaceSymbolTriggerDebouncingTime`, 1 s by default) without
   `ProgressManager.checkCanceled()`, and then blocked on `Future.get()` for `LanguageServerManager.getLanguageServer`
   and `workspace/symbol`. A read action that never checks cancellation cannot be preempted, so the EDT's
   write action (completion start, document commit, …) waits for the whole debounce + LSP round trip.
3. `processElementsWithName` also lacked the `isLeanProject()` guard that `processNames` already had, so the
   same thing could start the Lean server from any project containing Markdown.

## Fix

`src/main/kotlin/lean4ij/language/Lean4WorkspaceSymbolContributor.kt`:

- `processElementsWithName` returns early when the project is not a Lean project.
- The debounce loop calls `ProgressManager.checkCanceled()` on every iteration.
- Both futures in `WorkspaceSymbolsCacheLoader.load` are awaited with `ProgressIndicatorUtils.awaitWithCheckCanceled`.
- `ProcessCanceledException` thrown by the loader (wrapped by guava into `UncheckedExecutionException`) is
  unwrapped and rethrown instead of being logged, so the platform retries the read action.

A cancelled lookup now returns no symbols for that read action; the next (retried) pass gets the cached result.
