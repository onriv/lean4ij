package com.github.onriv.ijpluginlean.lsp.data

import org.eclipse.lsp4j.TextDocumentIdentifier

/**
/-- `$/lean/plainGoal` client->server request.

If there is a tactic proof at the specified position, returns the current goals.
Otherwise returns `null`. -/
checking https://github.com/leanprover/lean4/blob/master/src/Lean/Server/FileWorker/WidgetRequests.lean#L91
the method
{
    "params": {
        "textDocument": {
            "uri": "file:///Users/river/Repos/mathematics_in_lean/MMIL/Common.lean"
        },
        "sessionId": "17091158675354988890",
        "position": {
            "line": 4,
            "character": 0
        },
        "params": {
            "textDocument": {
                "uri": "file:///Users/river/Repos/mathematics_in_lean/MMIL/Common.lean"
            },
            "position": {
                "line": 4,
                "character": 0
            }
        },
        "method": "Lean.Widget.getInteractiveGoals"
    },
    "method": "$/lean/rpc/call",
    "jsonrpc": "2.0",
    "id": 9
}
 also use this params
*/
class PlainGoalParams(textDocument: TextDocumentIdentifier, position: Position) :
    TextDocumentPositionParams(textDocument, position)

/**
/-- `$/lean/rpc/call` client->server request.

A request to execute a procedure bound for RPC. If an incorrect session ID is present, the server
errors with `RpcNeedsReconnect`.

Extending TDPP is weird. But in Lean, symbols exist in the context of a position within a source
file. So we need this to refer to code in the environment at that position. -/
 TODO it seems there is duplicated params here...
 TODO Connect return Uint64 but here it's String... Although the source is Uint64...
*/
class RpcCallParams(
    val sessionId : String,
    val method: String,
    val params: Any,
    textDocument: TextDocumentIdentifier,
    position: Position) :
TextDocumentPositionParams(textDocument, position)
