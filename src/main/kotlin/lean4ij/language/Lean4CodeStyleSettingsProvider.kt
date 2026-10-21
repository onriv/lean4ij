package lean4ij.language

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

/**
 * Adds the "Code Style | Lean 4" settings page. Only the "Tabs and Indents" tab is shown, with the
 * Lean convention of two-space indentation as the default.
 */
class Lean4CodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {

    override fun getLanguage(): Language = Lean4Language.INSTANCE

    override fun getConfigurableDisplayName(): String = "Lean 4"

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions
    ) {
        indentOptions.TAB_SIZE = 2
        indentOptions.INDENT_SIZE = 2
        indentOptions.CONTINUATION_INDENT_SIZE = 2
        indentOptions.USE_TAB_CHARACTER = false
    }

    override fun getIndentOptionsEditor(): IndentOptionsEditor = IndentOptionsEditor(this)

    override fun getCodeSample(settingsType: SettingsType): String = CODE_SAMPLE

    override fun createConfigurable(
        baseSettings: CodeStyleSettings,
        modelSettings: CodeStyleSettings
    ): CodeStyleConfigurable {
        return object : CodeStyleAbstractConfigurable(baseSettings, modelSettings, configurableDisplayName) {
            override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel =
                Lean4CodeStylePanel(currentSettings, settings)
        }
    }

    private class Lean4CodeStylePanel(currentSettings: CodeStyleSettings, settings: CodeStyleSettings) :
        TabbedLanguageCodeStylePanel(Lean4Language.INSTANCE, currentSettings, settings) {

        override fun initTabs(settings: CodeStyleSettings) {
            addIndentOptionsTab(settings)
        }
    }

    companion object {
        private val CODE_SAMPLE = """
            |def fib : Nat → Nat
            |  | 0 => 0
            |  | 1 => 1
            |  | n + 2 => fib n + fib (n + 1)
            |
            |theorem fib_pos (n : Nat) (h : 0 < n) : 0 < fib n := by
            |  induction n with
            |  | zero => contradiction
            |  | succ k ih =>
            |    cases k with
            |    | zero => decide
            |    | succ m => simp [fib]; omega
            |""".trimMargin()
    }
}
