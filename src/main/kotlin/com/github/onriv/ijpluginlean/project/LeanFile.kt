package com.github.onriv.ijpluginlean.project

import com.github.onriv.ijpluginlean.infoview.external.ExternalInfoViewService
import com.github.onriv.ijpluginlean.infoview.external.data.CursorLocation
import com.github.onriv.ijpluginlean.lsp.data.Position
import com.github.onriv.ijpluginlean.lsp.data.Range
import com.github.onriv.ijpluginlean.util.LspUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class LeanFile(private val project: Project, private val file: VirtualFile) {
    fun updateCaret(logicalPosition: LogicalPosition) {
        val position = Position(line=logicalPosition.line, character = logicalPosition.column)
        val cursorLocation = CursorLocation(LspUtil.quote(file.path), Range(position, position))
        project.service<ExternalInfoViewService>().changedCursorLocation(cursorLocation)
    }


}