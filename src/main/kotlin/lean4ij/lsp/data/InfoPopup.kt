package lean4ij.lsp.data

/**
 * for the return of infoToInteractive
 * see:
 * /-- The information that the infoview uses to render a popup
 * for when the user hovers over an expression.
 * -/
 * structure InfoPopup where
 *   type : Option CodeWithInfos
 *   /-- Show the term with the implicit arguments. -/
 *   exprExplicit : Option CodeWithInfos
 *   /-- Docstring. In markdown. -/
 *   doc : Option String
 *   deriving Inhabited, RpcEncodable
 * see: lean4/src/Lean/Server/FileWorker/WidgetRequests.lean:36:10
 */
class InfoPopup(
    val type: TaggedText<SubexprInfo>?,
    val exprExplicit: TaggedText<SubexprInfo>?,
    val doc: String?
)