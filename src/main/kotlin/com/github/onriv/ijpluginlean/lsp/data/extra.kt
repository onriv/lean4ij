package com.github.onriv.ijpluginlean.lsp.data

import com.github.onriv.ijpluginlean.lsp.Range
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import io.opentelemetry.sdk.trace.data.StatusData
import org.eclipse.lsp4j.TextDocumentIdentifier
import java.lang.reflect.Type

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

/**
 * TODO DRY
 */
class InteractiveInfoParams(
    val sessionId : String,
    val method: String,
    val params: ContextInfo,
    textDocument: TextDocumentIdentifier,
    position: Position) :
    TextDocumentPositionParams(textDocument, position)

// TODO this is Lean's source code's def, but the json seems to be just String
// data class FVarId (val name: String)

/**
 * TODO weird, this is not consistent with the Lean's code
 * but the json file seems it only has a p
 */
data class ContextInfo(val p : String)

// see: tests/lean/interactive/run.lean:11
data class SubexprInfo (val subexprPos: String, val info: ContextInfo, val diffStatus: String?)

/**
 * from
 * /-- The minimal structure needed to represent "string with interesting (tagged) substrings".
 * Much like Lean 3 [`sf`](https://github.com/leanprover-community/mathlib/blob/bfa6bbbce69149792cc009ab7f9bc146181dc051/src/tactic/interactive_expr.lean#L38),
 * but with indentation already stringified. -/
 * inductive TaggedText (α : Type u) where
 *   | text   : String → TaggedText α
 *   /-- Invariants:
 *   - non-empty
 *   - no adjacent `text` elements (they should be collapsed)
 *   - no directly nested `append`s (but `append #[tag _ (append ..)]` is okay) -/
 *   | append : Array (TaggedText α) → TaggedText α
 *   | tag    : α → TaggedText α → TaggedText α
 *   deriving Inhabited, BEq, Repr, FromJson, ToJson
 *   in file src\Lean\Widget\TaggedText.lean
 * and
 *
/-- Pretty-printed syntax (usually but not necessarily an `Expr`) with embedded `Info`s. -/
abbrev CodeWithInfos := TaggedText SubexprInfo
 from src/Lean/Widget/InteractiveCode.lean:45
 */
abstract class CodeWithInfos {
    @Transient
    var startOffset : Int = -1

    @Transient
    var endOffset : Int = -1

    @Transient
    var codeText : String = ""

    @Transient
    var parent : CodeWithInfos? = null

    abstract fun toInfoViewString(startOffset: Int, parent : CodeWithInfos?) : String

    // TODO kotlin way to do this
//    fun getStartOffset() = startOffset
//
//    // TODO kotlin way to do this
//    fun getEndOffset() = endOffset

    // TODO kotlin way to do this
    abstract fun getCodeText(offset: Int) : CodeWithInfos?

}
class CodeWithInfosText (val text: String) : CodeWithInfos() {

    override fun toInfoViewString(startOffset: Int, parent: CodeWithInfos?) : String {
        this.parent = parent
        this.startOffset = startOffset
        this.endOffset = startOffset+text.length
        this.codeText = text
        return text
    }

    override fun getCodeText(offset: Int) : CodeWithInfos? {
        return this
    }
}

class CodeWithInfosAppend (val append: List<CodeWithInfos>) : CodeWithInfos() {
    override fun toInfoViewString(startOffset: Int, parent: CodeWithInfos?) : String {
        this.parent = parent
        this.startOffset = startOffset
        val sb = StringBuilder()
        for (c in append) {
            sb.append(c.toInfoViewString(startOffset+sb.length, this))
        }
        this.endOffset=startOffset+sb.length
        this.codeText = sb.toString()
        return this.codeText
    }

    override fun getCodeText(offset: Int): CodeWithInfos? {
        for (c in append) {
            if (c.startOffset <= offset && offset < c.endOffset) {
                return c.getCodeText(offset)
            }
        }
        return null
    }
}

class CodeWithInfosTag (val f0: SubexprInfo, val f1: CodeWithInfos) : CodeWithInfos() {
    override fun toInfoViewString(startOffset: Int, parent: CodeWithInfos?) : String {
        this.parent = parent
        // TODO handle events
        this.startOffset = startOffset
        this.codeText = f1.toInfoViewString(startOffset, this)
        this.endOffset = startOffset+this.codeText.length
        return this.codeText
    }

    override fun getCodeText(offset: Int): CodeWithInfos? {
        return f1.getCodeText(offset)
    }
}

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
 class InteractiveGoal(
    val userName: String? = null,
    val type: CodeWithInfos,
    val mvarId: String,
    val isInserted: Boolean? = null,
    val hyps: Array<InteractiveHypothesisBundle>,
    val ctx: ContextInfo,
    val goalPrefix: String,
    val isRemoved: Boolean? = null) {

    @Transient
     private var startOffset : Int = -1

    @Transient
    private var endOffset : Int = -1

     fun toInfoViewString(startOffset: Int) : String {
         this.startOffset = startOffset
         val sb = StringBuilder()
         if (userName != null) {
             sb.append("case $userName\n")
         }
         // TODO hyps
         sb.append("⊢ ")
         sb.append("${type.toInfoViewString(startOffset+sb.length, null)}\n")
         this.endOffset = startOffset+sb.count()
         return sb.toString()
     }

    // TODO try DIY this
    // TODO kotlin way to do this
    fun getStartOffset() = startOffset

    // TODO kotlin way to do this
    fun getEndOffset() = endOffset

    fun getCodeText(offset: Int) : CodeWithInfos? {
        return type.getCodeText(offset)
    }
}

class InteractiveGoals(
    val goals : List<InteractiveGoal>) {

    /**
     * This is from https://github.com/Julian/lean.nvim/blob/03f7437/lua/lean/infoview/components.lua
     */
    fun toInfoViewString() : String {
        val sb = StringBuilder()
        if (goals.isEmpty()) {
            return "goals accomplished 🎉\n"
        }
        sb.append("${goals.size} goals\n")
        for (goal in goals) {
            sb.append(goal.toInfoViewString(sb.length))
        }
        return sb.toString()
    }

    // TODO all these and above can be ut
    fun getCodeText(offset : Int) : CodeWithInfos? {
        for (goal in goals) {
            if (goal.getStartOffset() <= offset && offset < goal.getEndOffset()) {
                return goal.getCodeText(offset)
            }
        }
        return null
    }
}

val gson = GsonBuilder()
    .registerTypeAdapter(CodeWithInfos::class.java, object : JsonDeserializer<CodeWithInfos> {
        override fun deserialize(p0: JsonElement?, p1: Type?, p2: JsonDeserializationContext?): CodeWithInfos? {
            if (p0 == null) {
                return null
            }
            if (p0.isJsonObject && p0.asJsonObject.has("tag")) {
                @Suppress("NAME_SHADOWING")
                val p1 = p0.asJsonObject.getAsJsonArray("tag")

                @Suppress("NAME_SHADOWING")
                val p2 = p2!!
                val f0: SubexprInfo = p2.deserialize(p1.get(0), SubexprInfo::class.java)
                val f1: CodeWithInfos = p2.deserialize(p1.get(1), CodeWithInfos::class.java)
                return CodeWithInfosTag(f0, f1)
            }
            if (p0.isJsonObject && p0.asJsonObject.has("append")) {
                @Suppress("NAME_SHADOWING")
                val p1 = p0.asJsonObject.getAsJsonArray("append")

                @Suppress("NAME_SHADOWING")
                val p2 = p2!!
                val r: MutableList<CodeWithInfos> = ArrayList()
                for (e in p1) {
                    r.add(p2.deserialize(e, CodeWithInfos::class.java))
                }
                return CodeWithInfosAppend(r)
            }
            if (p0.isJsonObject && p0.asJsonObject.has("text")) {
                @Suppress("NAME_SHADOWING")
                val p1 = p0.asJsonObject.getAsJsonPrimitive("text").asString
                return CodeWithInfosText(p1)
            }
            throw IllegalStateException(p0.toString())
        }
    })
    .create()