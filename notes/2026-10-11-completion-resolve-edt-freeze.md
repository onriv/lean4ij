# 2026-10-11 — IDE freeze while typing with the completion popup open (IDEA 2027.1 EAP 271.331, LSP4IJ 0.21.0)

## Symptom

The whole IDE froze for ~35 s while typing in `MyTest/Basic.lean` with the lookup (completion popup) open.
`threadDumps-freeze-20261011-225214-IU-271.331/report.txt`: the EDT spent the whole time in one frame.

## Stack (EDT, abbreviated)

```
"AWT-EventQueue-0" waiting on condition
	at java.util.concurrent.CompletableFuture.timedGet
	at com.intellij.openapi.progress.util.ProgressIndicatorUtils.awaitWithCheckCanceled(ProgressIndicatorUtils.java:379)
	at com.redhat.devtools.lsp4ij.client.features.LSPCompletionProposal.getResolvedCompletionItem(LSPCompletionProposal.java:500)
	at com.redhat.devtools.lsp4ij.client.features.LSPCompletionProposal.getExpensiveRenderer(LSPCompletionProposal.java:281)
	at com.intellij.codeInsight.lookup.LookupElementDecorator.getExpensiveRenderer
	at com.intellij.codeInsight.lookup.impl.LookupCellRenderer.updateItemPresentation(LookupCellRenderer.kt:640)
	at com.intellij.codeInsight.lookup.impl.LookupCellRenderer.scheduleVisibleItemsExpensiveRendering(LookupCellRenderer.kt:607)
	at com.intellij.codeInsight.lookup.impl.LookupImpl.updateList(LookupImpl.java:638)
	at com.intellij.codeInsight.lookup.impl.LookupImpl.refreshUi(LookupImpl.java:1534)
	at com.intellij.codeInsight.lookup.impl.LookupImpl.appendPrefix(LookupImpl.java:515)
	at com.intellij.codeInsight.lookup.impl.LookupTypedHandler.beforeCharTyped(LookupTypedHandler.java:109)
	...
	at com.intellij.openapi.editor.actionSystem.TypedAction.actionPerformed(TypedAction.java:55)
	at com.intellij.openapi.editor.impl.EditorImpl.processKeyTyped
```

## Root cause

1. `LSPCompletionProposal.getExpensiveRenderer()` (LSP4IJ) is called on the EDT for the selected lookup item on
   every typed character (`LookupImpl.appendPrefix -> refreshUi -> updateList`). When the item has no `detail`
   and the server supports `completionItem/resolve`, it *blocks* on the resolve future with
   `awaitWithCheckCanceled` before returning the renderer. On the EDT there is no progress indicator, so
   `checkCanceled` is a no-op and the wait is unbounded.
2. The Lean server returns completion items without `detail` (the type signature is filled by `completionItem/resolve`),
   and its resolve handler waits until the file is elaborated up to the completion position. Each keystroke
   restarts elaboration, so the response arrives only when elaboration catches up; with a slow file this is tens of seconds.
3. The same blocking call also happens in `LSPCompletionProposal.handleInsert()` when
   `LSPCompletionFeature.shouldResolveOnApply()` returns true (the default), i.e. accepting an item could freeze too.

The platform runs the renderer *returned* by `getExpensiveRenderer()` in a background, cancellable read action
(`com.intellij.codeInsight.lookup.impl.AsyncRendering`); only LSP4IJ's choice to wait *before* returning it is wrong.

## Fix (plugin side)

- `lean4ij.lsp.LeanCompletionProposal` (new): subclass of `LSPCompletionProposal` whose `getExpensiveRenderer()`
  returns immediately; the renderer itself sends `completionItem/resolve` through the plugin's own
  `LeanProjectService.languageServer` handle, waits with `awaitWithCheckCanceled` in the background read action
  (a write action cancels it; the pending future is cached and reused on the next render), then copies `detail`
  into the item and renders. Only the selected item is resolved, like LSP4IJ.
- `LeanLSPCompletionFeature.createLookupElement` now creates `LeanCompletionProposal` instead of calling the
  base implementation (the `textEdit` workaround is kept).
- `LeanLSPCompletionFeature.shouldResolveOnApply` returns `false`: the Lean resolve handler only fills
  `detail`/`documentation`, no `additionalTextEdits` or commands, so nothing is lost.

Quick documentation in the lookup still uses LSP4IJ's own resolve path (`computeDocumentation`), which runs
in a background thread and was not involved in the freeze.

## Upstream

LSP4IJ issue: `LSPCompletionProposal.getExpensiveRenderer` blocks the EDT on `completionItem/resolve`
(https://github.com/redhat-developer/lsp4ij). The plugin-side workaround can be removed once LSP4IJ moves
the wait into the renderer.
