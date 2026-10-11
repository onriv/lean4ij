# 2026-10-11 — "Assertion failed" in `EditorImpl.removeEditorMouseMotionListener` when opening another file

## Symptom

Opening a second Lean file (`Classical.lean`) while hover highlighting was active logged:

```
java.lang.Throwable: Assertion failed
    at com.intellij.openapi.diagnostic.Logger.assertTrue(Logger.java:476)
    at com.intellij.openapi.diagnostic.Logger.assertTrue(Logger.java:485)
    at com.intellij.openapi.editor.impl.EditorImpl.removeEditorMouseMotionListener(EditorImpl.java:2452)
    at lean4ij.project.LeanProjectService.highlightCurrentContent(LeanProjectService.kt:302)
    at lean4ij.lsp.LeanLanguageServerLifecycleListener.handleLSPMessage(LeanLanguageServerLifecycleListener.kt:64)
    at lean4ij.lsp.LeanLanguageServerLifecycleListenerProxyFactory.create$lambda$0(LeanLanguageServerLifecycleListener.kt:106)
    at com.redhat.devtools.lsp4ij.lifecycle.LanguageServerLifecycleManager.logLSPMessage(LanguageServerLifecycleManager.java:77)
    at com.redhat.devtools.lsp4ij.LanguageServerWrapper.logMessage(LanguageServerWrapper.java:624)
    ...
    at org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer.listen(StreamMessageProducer.java:97)
```

## Root cause

`LeanProjectService.highlightCurrentContent` kept a single `hoverListener` / `hoverRangeHighlighter`, added them
to `FileEditorManager.selectedTextEditor`, and on the next hover response removed them again from *the currently
selected* editor. When the selection changed in between (another file opened), the listener had never been
registered on that editor and `EditorImpl.removeEditorMouseMotionListener` fails its `LOG.assertTrue(removed)`.
The surrounding `try/catch` could not suppress it: `Logger.assertTrue` reports the error via `LOG.error` before
anything is thrown, so it still ends up as a red exception in the IDE.

The same mismatch applied to `markupModel.removeHighlighter(hoverRangeHighlighter)` on a foreign markup model
(`IntervalTreeImpl.checkBelongsToTheTree` assertion that was already being caught in the old code).

Additionally the whole thing ran on the LSP reader thread instead of EDT.

## Fix

- Remember the editor the listener/highlighter were attached to (`hoverEditor`) and detach from *that* editor
  (skipping it when disposed), not from the selected one.
- Use `RangeHighlighter.dispose()` instead of `MarkupModel.removeHighlighter` — it works with any owning model
  and is a no-op when already invalid.
- Schedule the work on EDT via `scope.launch(Dispatchers.EDT)` and clamp the hover range to the document length
  (LSP ranges can be stale after an edit).
