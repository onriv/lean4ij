package lean4ij.project

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.FillingLineMarkerRenderer
import com.intellij.openapi.editor.markup.LineMarkerRendererEx

/**
 * TODO removed this?
 * TODO this requires refactor
 */
object leanFileProgressFinishedFillingLineMarkerRender : FillingLineMarkerRenderer {
    override fun getPosition(): LineMarkerRendererEx.Position {
        return LineMarkerRendererEx.Position.LEFT
    }

    /**
     * TODO the reason for keys is that it's related to theme
     *      use our own key here
     */
    private val textAttributesKey = TextAttributesKey.createTextAttributesKey("LEAN_FILE_PROGRESS_FINISHED")
    // private val textAttributesKey = TextAttributesKey.createTextAttributesKey("LINE_PARTIAL_COVERAGE")

    override fun getTextAttributesKey(): TextAttributesKey {
        return textAttributesKey
    }

}