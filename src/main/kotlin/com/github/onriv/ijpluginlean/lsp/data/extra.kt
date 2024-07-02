package com.github.onriv.ijpluginlean.lsp.data

import com.github.onriv.ijpluginlean.lsp.Range
import io.opentelemetry.sdk.trace.data.StatusData
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

class PlainTermGoalParams(textDocument: TextDocumentIdentifier, position: Position) :
    TextDocumentPositionParams(textDocument, position)

class PlainTermGoal(val goal : String, val range: Range)


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


class InteractiveGoalsParams(
    val sessionId : String,
    val method: String,
    val params: PlainGoalParams,
    textDocument: TextDocumentIdentifier,
    position: Position) :
    TextDocumentPositionParams(textDocument, position)

// TODO this is Lean's source code's def, but the json seems to be just String
// data class FVarId (val name: String)

// see: tests/lean/interactive/run.lean:11
data class SubexprInfo (val subexprPos: String, val dataStatus: String)

interface CodeWithInfos
data class CodeWithInfosText (val text: String) : CodeWithInfos
data class CodeWithInfosAppend (val append: List<CodeWithInfos>) : CodeWithInfos
data class CodeWithInfosTag (val a1: SubexprInfo, val a2: CodeWithInfos) : CodeWithInfos

// from src/Lean/Widget/InteractiveGoal.lean:51
data class InteractiveHypothesisBundle(
    val names: List<String>,
    val fvarIds: List<String>,
    val type: CodeWithInfos,
    val value: CodeWithInfos? = null,
    val isInstance: Boolean? = null,
    val isType: Boolean? = null,
    val isInserted: Boolean? = null,
    val isRemoved: Boolean? = null
)

//data class InteractiveGoalCore(
//    val hyps: Array<InteractiveHypothesisBundle>,
//    val type: CodeWithInfos,
//    val ctx: ContextInfo
//)
//
//data class InteractiveGoal(
//    val hyps: Array<InteractiveHypothesisBundle>,
//    val type: CodeWithInfos,
//    val ctx: ContextInfo,
//    val userName: String? = null,
//    val goalPrefix: String,
//    val mvarId: MVarId,
//    val isInserted: Boolean? = null,
//    val isRemoved: Boolean? = null
//)