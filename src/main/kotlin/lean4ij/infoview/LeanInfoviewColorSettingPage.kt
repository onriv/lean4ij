package lean4ij.infoview

import com.intellij.openapi.diff.DiffColors
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import java.awt.Color
import javax.swing.Icon

enum class AttrSelect {
    FOREGROUND,
    BACKGROUNND,
    COLOR,
    ALL,
}

fun createTextAttributesKey(color: Color?, fallbackKey: TextAttributesKey?) {
    if (color != null) {
        val key = TextAttributesKey.createTextAttributesKey("todo")
    }
}

enum class TextAttributesKeys(val style: String, private val fallbackKey: TextAttributesKey,
                              val attrSelect: AttrSelect,
                              val key: TextAttributesKey = TextAttributesKey.createTextAttributesKey(style, fallbackKey)) {
    Header("header", DefaultLanguageHighlighterColors.CLASS_NAME, AttrSelect.FOREGROUND),
    GoalHyp("goal-hyp", DefaultLanguageHighlighterColors.INSTANCE_FIELD, AttrSelect.FOREGROUND),
    GoalInaccessible("goal-inaccessible", DefaultLanguageHighlighterColors.LINE_COMMENT, AttrSelect.FOREGROUND),
    // The default color of the following two seems not bad
    InsertedText("inserted-text", DiffColors.DIFF_INSERTED, AttrSelect.BACKGROUNND),
    RemovedText("removed-text", DiffColors.DIFF_DELETED, AttrSelect.BACKGROUNND),

    SwingInfoviewCurrentPosition("swing-infoview-current-position", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE, AttrSelect.ALL),
    SwingInfoviewExpectedType("swing-infoview-expected-type", DefaultLanguageHighlighterColors.CLASS_NAME, AttrSelect.ALL),
    SwingInfoviewGoalSymbol("swing-infoview-goal-symbol", DefaultLanguageHighlighterColors.STRING, AttrSelect.ALL),
    SwingInfoviewCasePos("swing-infoview-case-pos", DefaultLanguageHighlighterColors.HIGHLIGHTED_REFERENCE, AttrSelect.ALL),
    SwingInfoviewAllMessagePos("swing-infoview-all-message-pos", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION, AttrSelect.ALL),
    SwingInfoviewAllMessageErrorPos("swing-infoview-all-message-error-pos", CodeInsightColors.RUNTIME_ERROR, AttrSelect.ALL),
    SwingInfoviewAllMessageSorryPos("swing-infoview-all-message-sorry-pos", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE, AttrSelect.ALL),
    SwingInfoviewAllMessageUnsupportedPos("swing-infoview-all-message-unsupported-pos", HighlighterColors.BAD_CHARACTER, AttrSelect.ALL),
}

/**
 * Ref: [com.intellij.openapi.options.colors.pages.CustomColorsPage]
 * and [com.intellij.ide.highlighter.custom.CustomFileHighlighter]
 */
class LeanInfoviewColorSettingPage : ColorSettingsPage {

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        val keys = TextAttributesKeys.entries
            .associateBy({ it.style }, { it.key })
            .toMutableMap()
    }

    override fun getAttributeDescriptors() = keys.map { e ->  AttributesDescriptor(e.key, e.value)}.toTypedArray()

    override fun getColorDescriptors(): Array<ColorDescriptor> {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    override fun getDisplayName(): String {
        return "Lean Infoview"
    }

    /**
     * TODO define an icon?
     */
    override fun getIcon(): Icon? {
        return null
    }

    override fun getHighlighter(): SyntaxHighlighter {
        return PlainSyntaxHighlighter()
    }

    override fun getDemoText() = """
    <goal-hyp>R</goal-hyp> : Type u_1
    <goal-inaccessible>inst✝</goal-inaccessible> : Ring R
    <goal-hyp>a</goal-hyp> : R
    ⊢ <inserted-text>a * (0 + 0)</inserted-text> = <removed-text>a * 0 + 0</removed-text>
    """.trimIndent()

    @OptIn(ExperimentalStdlibApi::class)
    override fun getAdditionalHighlightingTagToDescriptorMap() = keys
}