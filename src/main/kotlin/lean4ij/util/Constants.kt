package lean4ij.util

object Constants {
    /**
     * This is duplicated in plugin.xml
     */
    const val LEAN_LANGUAGE_SERVER_ID = "lean"
    const val LEAN_LANGUAGE_ID = "lean"

    const val LEAN_PLAIN_GOAL = "\$/lean/plainGoal"
    const val LEAN_PLAIN_TERM_GOAL = "\$/lean/plainTermGoal"
    const val LEAN_RPC_CONNECT = "\$/lean/rpc/connect"
    const val LEAN_RPC_CALL = "\$/lean/rpc/call"
    const val LEAN_RPC_KEEP_ALIVE = "\$/lean/rpc/keepAlive"
    const val RPC_METHOD_INFO_TO_INTERACTIVE = "Lean.Widget.InteractiveDiagnostics.infoToInteractive"
    const val RPC_METHOD_GET_INTERACTIVE_GOALS = "Lean.Widget.getInteractiveGoals"
    const val RPC_METHOD_GET_INTERACTIVE_TERM_GOAL = "Lean.Widget.getInteractiveTermGoal"
    const val RPC_METHOD_GET_INTERACTIVE_DIAGNOSTICS = "Lean.Widget.getInteractiveDiagnostics"
    const val RPC_METHOD_LAZY_TRACE_CHILDREN_TO_INTERACTIVE = "Lean.Widget.lazyTraceChildrenToInteractive"
    const val FILE_PROGRESS = "\$/lean/fileProgress"

    /**
     * This is duplicated in App.tsx
     */
    const val EXTERNAL_INFOVIEW_SERVER_INITIALIZED = "serverInitialized"
    const val EXTERNAL_INFOVIEW_CHANGED_CURSOR_LOCATION = "changedCursorLocation"
}