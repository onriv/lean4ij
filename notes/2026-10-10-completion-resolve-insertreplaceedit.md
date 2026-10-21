# 2026-10-10 — `completionItem/resolve` rejected by the Lean server (IDEA 2027.1 EAP, build 271.331, LSP4IJ 0.21.0)

## Symptom

Three SEVERE entries in `idea.log` within 100 ms while a completion popup was open in `MyTest/Basic.lean`
(last action `EditorBackSpace`), reported against `c.i.u.SingleAlarm`, `UnhandledExceptions` and
`NonBlockingFlushQueue`. "Plugin to blame: LSP4IJ", but the root cause is in lean4ij.

```
java.lang.RuntimeException: java.util.concurrent.ExecutionException:
  org.eclipse.lsp4j.jsonrpc.ResponseErrorException: Cannot parse request params:
  {"data":["file:///.../MyTest/Basic.lean",30,34,0,"csemiOutParam"],"kind":3,"label":"semiOutParam",
   "textEdit":{"newText":"semiOutParam","range":{"end":{"character":34,"line":30},"start":{"character":32,"line":30}}}}
Lean.Lsp.CompletionItem.textEdit?: Lean.Lsp.InsertReplaceEdit.insert: Lean.Lsp.Range.start: Lean.Lsp.Position.line: Natural number expected
	at com.intellij.util.ExceptionUtil.rethrow(ExceptionUtil.java:175)
	at com.intellij.openapi.progress.util.ProgressIndicatorUtilsCore.awaitWithCheckCanceled(utils.kt:88)
	at com.redhat.devtools.lsp4ij.client.features.LSPCompletionProposal.getResolvedCompletionItem(LSPCompletionProposal.java:500)
	at com.redhat.devtools.lsp4ij.client.features.LSPCompletionProposal.getExpensiveRenderer(LSPCompletionProposal.java:281)
	at com.intellij.codeInsight.lookup.impl.LookupCellRenderer.updateItemPresentation(LookupCellRenderer.kt:640)
	at com.intellij.codeInsight.lookup.impl.LookupImpl.scheduleItemUpdate(LookupImpl.java:433)
	at com.redhat.devtools.lsp4ij.features.completion.LSPCompletionContributor$LSPLookupManagerListener$1.currentItemChanged(LSPCompletionContributor.java:273)
	...
Caused by: org.eclipse.lsp4j.jsonrpc.ResponseErrorException: Cannot parse request params: ...
	at org.eclipse.lsp4j.jsonrpc.RemoteEndpoint.handleResponse(RemoteEndpoint.java:214)
```

## Root cause

`lean4ij.lsp.LeanLSPCompletionFeature.createLookupElement` fills in a `textEdit` when the server did not
provide one (workaround for LSP4IJ inserting the completion at offset 0). It built an lsp4j `TextEdit`
(`{range, newText}`). LSP4IJ later sends the *same, mutated* `CompletionItem` back to the server in
`completionItem/resolve` (lazily, when the item is rendered/selected in the lookup).

The Lean server's `Lean.Lsp.CompletionItem` declares `textEdit? : Option InsertReplaceEdit`
(`{newText, insert, replace}`), not the `TextEdit | InsertReplaceEdit` union of the LSP spec. Deserializing
`{range, newText}` fails on the missing `insert` field, the server returns a JSON-RPC error, LSP4IJ rethrows
it on the EDT via `awaitWithCheckCanceled`, and the IDE reports it as an unhandled exception.

## Fix

Build an `InsertReplaceEdit(newText, insert = range, replace = range)` and store it as
`Either.forRight(...)`. LSP4IJ's `LSPCompletionProposal` handles both branches and uses
`InsertReplaceEdit.getInsert()` as the edit range (verified with `javap` on lsp4ij-0.21.0), so the original
workaround keeps working, and the round-tripped item now matches the server's schema.
