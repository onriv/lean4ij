package lean4ij.language

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.IconLoader.getIcon
import javax.swing.Icon


class Lean4Language : Language(PlainTextLanguage.INSTANCE, "lean4") {

    companion object {
        val INSTANCE = Lean4Language();
    }

}

class Lean4Icons {
    companion object {
        // check https://plugins.jetbrains.com/docs/intellij/icons.html#mapping-entries for making icons to respect theme
        val FILE = getIcon("/icons/lean_icon.svg", Lean4Icons::class.java);
    }
}

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