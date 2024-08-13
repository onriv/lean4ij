package lean4ij.lsp.data

/**
 * Lean declares sessionId as UInt32, it's kind of hard to work with in Java/Kotlin
 * like: 17710504432720554099 exit long
 * see: src/Lean/Data/Lsp/Extra.lean:124 to
 */
data class RpcConnected(val sessionId: String)