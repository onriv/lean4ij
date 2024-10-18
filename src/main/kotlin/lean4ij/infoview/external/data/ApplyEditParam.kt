package lean4ij.infoview.external.data

import lean4ij.lsp.data.Range

data class ApplyEditChange(val range: Range, val newText: String)

data class ApplyEditParam(val changes: Map<String, List<ApplyEditChange>>)