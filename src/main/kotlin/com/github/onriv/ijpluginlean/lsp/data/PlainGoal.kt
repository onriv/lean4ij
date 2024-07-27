package com.github.onriv.ijpluginlean.lsp.data

/**
 * `$/lean/plainGoal` client<-server reply.
 * see: [src/Lean/Data/Lsp/Extra.lean#L90](https://github.com/leanprover/lean4/blob/23e49eb519a45496a9740aeb311bf633a459a61e/src/Lean/Data/Lsp/Extra.lean#L90)
 */
data class PlainGoal(val rendered: String, val goals: List<String>)