package lean4ij.lsp.data

import org.eclipse.lsp4j.TextDocumentIdentifier

/**
`$/lean/plainGoal` client->server request.

If there is a tactic proof at the specified position, returns the current goals. Otherwise, returns `null`.

see [lean4/blob/master/src/Lean/Server/FileWorker/WidgetRequests.lean#L91](https://github.com/leanprover/lean4/blob/master/src/Lean/Server/FileWorker/WidgetRequests.lean#L91)
*/
class PlainGoalParams(textDocument: TextDocumentIdentifier, position: Position) :
    TextDocumentPositionParams(textDocument, position)