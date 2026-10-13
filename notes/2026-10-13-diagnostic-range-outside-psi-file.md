# 2026-10-13 — "Range must be inside element being annotated" from LSP4IJ diagnostics while typing

## Symptom

While editing the end of `Quantifiers.lean` (last actions: `EditorBackSpace`, `EditorRight`, plain typing),
idea.log got one of these per keystroke (IDEA 2027.1 EAP 271.331, LSP4IJ 0.21.0, lean4ij 0.2.9-beta.1):

```
SEVERE - #o.j.c.AsyncPromise - Range must be inside element being annotated: (0,5096); but got: (5096,5097)
com.intellij.diagnostic.PluginException: Range must be inside element being annotated: (0,5096); but got: (5096,5097)
    at com.intellij.codeInsight.daemon.impl.B.range(B.java:200)
    at com.redhat.devtools.lsp4ij.client.features.LSPDiagnosticFeature.createAnnotation(LSPDiagnosticFeature.java:110)
    at com.redhat.devtools.lsp4ij.features.diagnostics.LSPDiagnosticsCollector.collect(LSPDiagnosticsCollector.java:122)
    at com.redhat.devtools.lsp4ij.features.diagnostics.LSPDiagnosticsApplier.collectDiagnostics(LSPDiagnosticsApplier.java:129)
    at com.redhat.devtools.lsp4ij.features.diagnostics.LSPDiagnosticsApplier.lambda$scheduleRefresh$0(LSPDiagnosticsApplier.java:76)
    at com.intellij.openapi.application.impl.NonBlockingReadActionImpl$OTelMonitor.callWrapped(...)
```

The reported range is always `(N, N+1)` where `N` is the length of the element being annotated (the whole
`PsiFile`).

## Root cause

LSP4IJ 0.21.0 `LSPDiagnosticsApplier.scheduleRefresh` runs `LSPDiagnosticsCollector.collect()` in
`ReadAction.nonBlocking(...)` **without** `withDocumentsCommitted(project)`. The collector:

1. converts each LSP `Range` to a `TextRange` with `LSPIJUtils.toTextRange(range, document, null, adjust = true)`,
   i.e. clamped against the **Document**;
2. feeds it to an `AnnotationHolderImpl` created for the **PsiFile**, whose `range(...)` asserts that the range lies
   inside the PSI element (`LOG.error` → the `PluginException` above).

While the user types, the Document is already one character longer than the uncommitted PSI tree
(`document.textLength == 5097`, `psiFile.textLength == 5096`). The Lean server reports several diagnostics anchored
at the end of the file (`unexpected end of input`, `unsolved goals` on the last declaration), so one of them maps to
`(5096, 5097)`: valid for the Document, invalid for the PSI file. The `catch (IndexOutOfBoundsException)` in the
collector does not help because the assertion is a logged `PluginException`, not a thrown exception.

This is an LSP4IJ bug (the collector should wait for committed documents or validate against the PSI file). It is
not yet fixed upstream as of 0.21.0 / the 0.21.1 nightly we build against.

## Fix (lean4ij side)

`lean4ij.lsp.LeanLSPDiagnosticFeature` (new) overrides `LSPDiagnosticFeature.createAnnotation`: it reproduces the
upstream body but clamps the range to `min(document.textLength, holder.currentAnnotationSession.file.textLength)`.
A range that starts beyond the limit is dropped (LSP4IJ discards the stale highlights anyway because the
Document modification stamp changed); a range that only ends beyond it is cut, and an empty range at EOF is shown
`afterEndOfLine`. `Lean4LSPClientFeatures` now installs this class instead of the anonymous
`isEnabled = true` subclass.

## Also seen in the same log (not a lean4ij bug)

~40 × `Read access is allowed from inside read-action only` on the EDT after `RenameElement`, blamed on
"Full Line Code Completion" / "Groovy":

```
at com.intellij.psi.impl.source.tree.TreeElement.assertReadAccessAllowed(TreeElement.java:857)
at com.intellij.psi.impl.source.tree.SharedImplUtil.getParent(SharedImplUtil.java:37)
at com.intellij.extapi.psi.ASTWrapperPsiElement.getParent(ASTWrapperPsiElement.java:24)
at org.jetbrains.plugins.groovy.geb.GebContentDeclarationSearcher.findDeclarationsAt(GebContentDeclarationSearcher.java:22)
at com.intellij.codeInsight.TargetElementUtilBase.getNamedElement(TargetElementUtilBase.java:149)
at com.intellij.codeInsight.TargetElementUtil.findTargetElement(TargetElementUtil.java:151)
at com.intellij.ml.llm.nextEdits.backend.history.refactoring.rename.NextEditFileHistoryRenameActionListener.findNamedElement(NextEditFileHistoryRenameActionListener.kt:81)
at com.intellij.ml.llm.nextEdits.backend.history.refactoring.rename.NextEditFileHistoryRenameActionListener.afterActionPerformed(NextEditFileHistoryRenameActionListener.kt:51)
at com.intellij.openapi.actionSystem.impl.ActionManagerImpl.fireAfterActionPerformed(ActionManagerImpl.kt:534)
```

The bundled Next Edit Suggestions listener (`com.intellij.ml.llm.nextEdits`) walks the PSI tree in
`AnActionListener.afterActionPerformed` on the EDT without a read action; on 271 the EDT no longer holds an
implicit write-intent lock there. It fires for any language after a rename, Lean PSI is only the victim. Nothing
to fix in lean4ij; disabling the "Full Line Code Completion" plugin in the sandbox IDE makes it go away.
