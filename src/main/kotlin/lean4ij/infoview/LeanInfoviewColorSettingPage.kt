package lean4ij.infoview

import com.intellij.openapi.diff.DiffColors
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

/**
 * Ref: [com.intellij.openapi.options.colors.pages.CustomColorsPage]
 * and [com.intellij.ide.highlighter.custom.CustomFileHighlighter]
 */
class LeanInfoviewColorSettingPage : ColorSettingsPage {

    companion object {
        val keys= mutableMapOf(
            "goal-hyp" to TextAttributesKey.createTextAttributesKey("LEAN_INFO_VIEW_GOAL_HYP", DefaultLanguageHighlighterColors.PARAMETER),
            "goal-inaccessible" to TextAttributesKey.createTextAttributesKey("LEAN_INFO_VIEW_GOAL_INACCESSIBLE", DefaultLanguageHighlighterColors.LINE_COMMENT),
            "inserted-text" to TextAttributesKey.createTextAttributesKey("LEAN_INFO_VIEW_INSERTED_TEXT", DiffColors.DIFF_INSERTED),
            "removed-text" to TextAttributesKey.createTextAttributesKey("LEAN_INFO_VIEW_REMOVED_TEXT", DiffColors.DIFF_DELETED)
        )
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

    override fun getAdditionalHighlightingTagToDescriptorMap() = keys
}