package lean4ij.language

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.progress.ProgressManager
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PlainTextTokenTypes
import com.intellij.util.ProcessingContext


class Lean4ManualCompletionProvider(private val onlyManual: Boolean) : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        // return early if we're not supposed to show items
        // in the automatic popup
        if (parameters.isAutoPopup/* && onlyManual*/) {
            // TODO why isAutoPopup return?
            // return
        }

        // return early when there's not prefix
        var prefix = result.prefixMatcher.prefix
        if (prefix.isEmpty()) {
            return
        }

        // make sure that our prefix is the last word
        // (In plain text files, the prefix initially contains the whole
        // file up to the cursor. We don't want that, as we're only
        // completing a single word.)
        val dictResult: CompletionResultSet
        val lastSpace = prefix.lastIndexOf(' ')
        if (lastSpace >= 0 && lastSpace < prefix.length - 1) {
            prefix = prefix.substring(lastSpace + 1)
            dictResult = result.withPrefixMatcher(prefix)
        } else {
            dictResult = result
        }

        val length = prefix.length
        val firstChar = prefix[0]
        val isUppercase = Character.isUpperCase(firstChar)

        // limit completions to 20 additional characters max
        arrayOf("#check", "def", "example", "theorem").forEach{ word ->
            // return early when the user modified the data of our editor
            ProgressManager.checkCanceled()

            val element: LookupElementBuilder
            if (isUppercase) {
                element = LookupElementBuilder.create(
                    word.substring(0, 1).uppercase() + word.substring(1)
                )
            } else {
                element = LookupElementBuilder.create(word)
            }

            // finally, add it to the completions
            dictResult.addElement(element)
        }

    }

}

/**
 * ref, from
 * https://www.plugin-dev.com/intellij/custom-language/code-completion/
 * and
 * https://github.com/jansorg/intellij-code-completion
 */
class Lean4ManualCompletionContributor: CompletionContributor() {

    init {
        extend(CompletionType.BASIC,
            PlatformPatterns.not(PlatformPatterns.alwaysFalse()),
            Lean4ManualCompletionProvider(false))
    }
}