package lean4ij.language

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.LiveTemplateLookupElementImpl
import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.codeInsight.template.impl.TemplateSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import lean4ij.setting.Lean4Settings

/**
 * Implements the "space only when expanded with Space" mode of [Lean4Settings.spaceAfterUnicodeAbbreviation]:
 * `\neg<Space>` inserts `¬ ` while `\neg<Tab>` (or Enter / a click in the completion popup) inserts `¬`.
 *
 * Only the templates of the `Lean4` group (generated from `abbreviations.json`) have no trailing space, and
 * a live template declared with `shortcut="SPACE"` swallows the typed space. So on Space this handler
 * expands the abbreviation itself, through a copy of the template with a trailing space, and stops the
 * platform's own expansion. Tab / Enter / click keep going through the lookup element, which inserts the
 * original template.
 *
 * There are two ways a Space can reach a template:
 *  - with no (or an untouched auto-popup) lookup the platform's `SpaceHandler` typed-handler delegate would
 *    expand it: [Lean4AbbreviationSpaceTypedHandler] runs before it (`order="first"`);
 *  - with a lookup open and a template item selected, `LiveTemplateCharFilter` would select the item and
 *    insert it without the space: [Lean4AbbreviationCharFilter] runs before it, remembers the selected
 *    template in [PENDING_ABBREVIATION] and hides the lookup, so that the Space falls through to the
 *    typed handler, which then expands the remembered item (and not just the text typed so far).
 */
private val PENDING_ABBREVIATION = Key.create<PendingAbbreviation>("lean4ij.pendingUnicodeAbbreviation")

private class PendingAbbreviation(
    val template: TemplateImpl,
    val prefixLength: Int,
    val caretOffset: Int,
    val modificationStamp: Long,
)

private const val UNICODE_ABBREVIATION_GROUP = "Lean4"

private fun spaceOnlyWithSpaceKey(): Boolean =
    service<Lean4Settings>().spaceAfterUnicodeAbbreviation == Lean4Settings.SPACE_ONLY_WITH_SPACE_KEY

private fun isUnicodeAbbreviation(template: TemplateImpl): Boolean =
    template.groupName == UNICODE_ABBREVIATION_GROUP

private fun isLeanFile(file: PsiFile?): Boolean {
    val name = file?.name ?: return false
    return name.endsWith(".lean") || name.endsWith(".lean4")
}

class Lean4AbbreviationCharFilter : CharFilter() {
    override fun acceptChar(c: Char, prefixLength: Int, lookup: Lookup): Result? {
        if (c != ' ' || !isLeanFile(lookup.psiFile) || !spaceOnlyWithSpaceKey()) return null
        val item = lookup.currentItem as? LiveTemplateLookupElementImpl ?: return null
        val template = item.template
        if (!isUnicodeAbbreviation(template)) return null
        val editor = lookup.editor
        editor.putUserData(
            PENDING_ABBREVIATION,
            PendingAbbreviation(template, prefixLength, editor.caretModel.offset, editor.document.modificationStamp)
        )
        // the typed space then goes through the regular typed handlers, see Lean4AbbreviationSpaceTypedHandler
        return Result.HIDE_LOOKUP
    }
}

class Lean4AbbreviationSpaceTypedHandler : TypedHandlerDelegate() {
    override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
        if (c != ' ') return Result.CONTINUE
        val pending = editor.getUserData(PENDING_ABBREVIATION)
        editor.putUserData(PENDING_ABBREVIATION, null)
        if (!isLeanFile(file) || !spaceOnlyWithSpaceKey()) return Result.CONTINUE
        if (editor.selectionModel.hasSelection() || editor.caretModel.caretCount != 1) return Result.CONTINUE

        val caretOffset = editor.caretModel.offset
        val match = if (pending != null
            && pending.caretOffset == caretOffset
            && pending.modificationStamp == editor.document.modificationStamp
        ) {
            pending.template to pending.prefixLength
        } else {
            findSingleMatchingAbbreviation(project, editor, file)?.let { it to it.key.length }
        } ?: return Result.CONTINUE

        val (template, prefixLength) = match
        val start = caretOffset - prefixLength
        if (start < 0) return Result.CONTINUE

        val withSpace = TemplateImpl(template.key, template.string + " ", template.groupName)
        withSpace.description = template.description
        // typed handlers run inside the write action of the typed action
        editor.document.deleteString(start, caretOffset)
        editor.caretModel.moveToOffset(start)
        editor.selectionModel.removeSelection()
        TemplateManager.getInstance(project).startTemplate(editor, withSpace)
        return Result.STOP
    }

    /**
     * The same lookup the platform does for a Space shortcut, restricted to the case of exactly one
     * candidate. With several candidates (e.g. a pair template with the same key) the platform's chooser
     * is kept.
     */
    private fun findSingleMatchingAbbreviation(project: Project, editor: Editor, file: PsiFile): TemplateImpl? {
        val manager = TemplateManager.getInstance(project) as? TemplateManagerImpl ?: return null
        PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        val candidates = manager.findMatchingTemplates(file, editor, TemplateSettings.SPACE_CHAR, TemplateSettings.getInstance())
            ?: return null
        if (candidates.size != 1) return null
        val (template, argument) = candidates.entries.single()
        if (argument != null || !isUnicodeAbbreviation(template)) return null
        return template
    }
}
