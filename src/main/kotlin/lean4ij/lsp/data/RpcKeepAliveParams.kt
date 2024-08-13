package lean4ij.lsp.data

/**
 * see Lean4's repo src/Lean/Data/Lsp/Extra.lean:157
 * the def for RpcKeepAliveParams:
 * The client must send an RPC notification every 10s in order to keep the RPC session alive.
 * This is the simplest one. On not seeing any notifications for three 10s periods, the server
 * will drop the RPC session and its associated references.
 */
data class RpcKeepAliveParams (val uri : String, val sessionId : String)