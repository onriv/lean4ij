package lean4ij.language

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.IconLoader.getIcon
import javax.swing.Icon


/**
 * Definition of the Lean4 Language.
 *
 * Inherits from [PlainTextLanguage] as a base, but provides specific support for Lean 4 syntax and features.
 */
class Lean4Language : Language(PlainTextLanguage.INSTANCE, "lean4") {

    companion object {
        val INSTANCE = Lean4Language();
    }

}

/**
 * Container for icons used in the Lean 4 plugin.
 */
class Lean4Icons {
    companion object {
        // check https://plugins.jetbrains.com/docs/intellij/icons.html#mapping-entries for making icons to respect theme
        val FILE = getIcon("/icons/lean_icon.svg", Lean4Icons::class.java);
    }
}

/**
 * The file type definition for Lean 4 files.
 *
 * Associates the `.lean` extension with [Lean4Language].
 */
object Lean4FileType : LanguageFileType(Lean4Language.INSTANCE) {
    override fun getName(): String {
        return "lean4"
    }

    override fun getDescription(): String {
        return "Lean4: programming language and theorem prover"
    }

    override fun getDefaultExtension(): String {
        return "lean"
    }

    override fun getIcon(): Icon {
        return Lean4Icons.FILE
        // return AllIcons.Debugger.LambdaBreakpoint
    }
}