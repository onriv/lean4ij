package lean4ij.util

/**
 * Central registry of string constants used throughout the plugin.
 *
 * ## LSP Method Names
 * Lean 4 extends the standard LSP protocol with custom methods prefixed with `$/lean/`.
 * These constants define the JSON-RPC method names for:
 * - **Plain goals/term goals**: Non-interactive goal display (`$/lean/plainGoal`, `$/lean/plainTermGoal`)
 * - **RPC session**: Session management for widget queries (`$/lean/rpc/connect`, `$/lean/rpc/call`,
 *   `$/lean/rpc/keepAlive`)
 * - **File progress**: Compilation progress notifications (`$/lean/fileProgress`)
 *
 * ## RPC Widget Methods
 * The `$/lean/rpc/call` endpoint is generic — the actual operation is determined by the `method`
 * field in the params. These constants define the supported widget RPC methods from
 * `Lean.Widget.*` namespace.
 *
 * ## External Infoview Events
 * Event type strings used in the WebSocket protocol between the embedded server and the
 * React-based infoview frontend.
 */
object Constants {
    /** Language server definition ID, must match the value in plugin.xml. */
    const val LEAN_LANGUAGE_SERVER_ID = "lean"
    /** Language ID for Lean files in the LSP protocol. */
    const val LEAN_LANGUAGE_ID = "lean"

    // -- Lean-specific LSP methods --
    const val LEAN_PLAIN_GOAL = "\$/lean/plainGoal"
    const val LEAN_PLAIN_TERM_GOAL = "\$/lean/plainTermGoal"
    const val LEAN_RPC_CONNECT = "\$/lean/rpc/connect"
    const val LEAN_RPC_CALL = "\$/lean/rpc/call"
    const val LEAN_RPC_KEEP_ALIVE = "\$/lean/rpc/keepAlive"

    // -- RPC widget method names (dispatched via $/lean/rpc/call) --
    const val RPC_METHOD_INFO_TO_INTERACTIVE = "Lean.Widget.InteractiveDiagnostics.infoToInteractive"
    const val RPC_METHOD_GET_INTERACTIVE_GOALS = "Lean.Widget.getInteractiveGoals"
    const val RPC_METHOD_GET_INTERACTIVE_TERM_GOAL = "Lean.Widget.getInteractiveTermGoal"
    const val RPC_METHOD_GET_INTERACTIVE_DIAGNOSTICS = "Lean.Widget.getInteractiveDiagnostics"
    const val RPC_METHOD_LAZY_TRACE_CHILDREN_TO_INTERACTIVE = "Lean.Widget.lazyTraceChildrenToInteractive"
    const val RPC_METHOD_GET_GOTO_LOCATION = "Lean.Widget.getGoToLocation"

    /** Notification method for file compilation progress. */
    const val FILE_PROGRESS = "\$/lean/fileProgress"

    // -- External infoview WebSocket event types (must match App.tsx) --
    const val EXTERNAL_INFOVIEW_SERVER_INITIALIZED = "serverInitialized"
    const val EXTERNAL_INFOVIEW_CHANGED_CURSOR_LOCATION = "changedCursorLocation"
}