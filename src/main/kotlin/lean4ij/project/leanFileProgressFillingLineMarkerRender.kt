package lean4ij.project

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.FillingLineMarkerRenderer
import com.intellij.openapi.editor.markup.LineMarkerRendererEx

/**
 * TODO this requires refactor
 */
object leanFileProgressFillingLineMarkerRender : FillingLineMarkerRenderer {
    override fun getPosition(): LineMarkerRendererEx.Position {
        // TODO right seems not working?
        return LineMarkerRendererEx.Position.LEFT
    }

    /**
     * TODO the reason for keys is that it's related to theme
     *      use our own key here
     */
    // private val textAttributesKey = TextAttributesKey.createTextAttributesKey("LEAN_FILE_PROGRESS")
    private val textAttributesKey = TextAttributesKey.createTextAttributesKey("LINE_PARTIAL_COVERAGE")

    override fun getTextAttributesKey(): TextAttributesKey {
        return textAttributesKey
    }

}