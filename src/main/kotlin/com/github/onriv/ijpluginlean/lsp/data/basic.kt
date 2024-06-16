/**
 * copy from https://github.com/leanprover/lean4/blob/master/src/Lean/Data/Lsp/Basic.lean
 */
package com.github.onriv.ijpluginlean.lsp.data

/**
 * lean doc:
/-- We adopt the convention that zero-based UTF-16 positions as sent by LSP clients
are represented by `Lsp.Position` while internally we mostly use `String.Pos` UTF-8
offsets. For diagnostics, one-based `Lean.Position`s are used internally.
`character` is accepted liberally: actual character := min(line length, character) -/
TODO dont know what the above doc mean
 */
class Position (
    line : Int,
    character : Int
)

class TextDocumentIdentifier (
    val url : String
)

open class TextDocumentPositionParams (
    val textDocument : TextDocumentIdentifier,
    val position : Position
)