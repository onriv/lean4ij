/**
 * copy from https://github.com/leanprover/lean4/blob/master/src/Lean/Data/Lsp/Basic.lean
 */
package com.github.onriv.ijpluginlean.lsp.data

import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * lean doc:
/-- We adopt the convention that zero-based UTF-16 positions as sent by LSP clients
are represented by `Lsp.Position` while internally we mostly use `String.Pos` UTF-8
offsets. For diagnostics, one-based `Lean.Position`s are used internally.
`character` is accepted liberally: actual character := min(line length, character) -/
TODO dont know what the above doc mean
 */
class Position (
    val line : Int,
    val character : Int
)

//class TextDocumentIdentifier (
//    val uri : String
//)
//
open class TextDocumentPositionParams (
    val textDocument : TextDocumentIdentifier?,
    val position : Position?
)