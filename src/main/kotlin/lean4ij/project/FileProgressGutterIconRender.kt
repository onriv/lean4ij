package lean4ij.project

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import javax.swing.Icon

/**
 * Renders a gutter icon indicating file progress status.
 *
 * This renderer is used as a fallback/alternative to the colored line marker approach
 * for showing Lean file elaboration progress in the editor gutter. Currently displays
 * a simple unique icon ([AllIcons.Gutter.Unique]).
 *
 * Note: The primary file progress visualization uses colored vertical bars via
 * [leanFileProgressFillingLineMarkerRender] and [leanFileProgressFinishedFillingLineMarkerRender].
 */
class FileProgressGutterIconRender : GutterIconRenderer() {
    override fun equals(other: Any?): Boolean {
        return other == this
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }

    override fun getIcon(): Icon {
        // return AllIcons.Actions.Refresh
        // intellij idea community plugin coverage has similar bar as vscode for
        // file progressing, nevertheless currently still just use an icon for
        // simpleness
        return AllIcons.Gutter.Unique
    }

}