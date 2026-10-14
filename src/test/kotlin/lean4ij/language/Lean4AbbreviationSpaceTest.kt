package lean4ij.language

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionContributorEP
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.template.impl.LiveTemplateCompletionContributor
import com.intellij.codeInsight.template.impl.LiveTemplateLookupElementImpl
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.PluginId
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import lean4ij.setting.Lean4Settings

/**
 * `\neg<Space>` must insert the symbol plus a space in the default mode, and nothing but the symbol in the
 * "never" mode (where the platform's own Space expansion applies).
 */
class Lean4AbbreviationSpaceTest : BasePlatformTestCase() {

    override fun setUp() {
        // An IU-only post-startup activity (obfuscated class, plugin com.intellij.modules.ultimate) fails to
        // instantiate in the test environment and would fail the test from the logger.
        LoggedErrorProcessor.executeWith<Throwable>(object : LoggedErrorProcessor() {
            override fun processError(category: String, message: String, details: Array<String>, t: Throwable?): Set<Action> {
                if (message.contains("Cannot create extension") && message.contains("com.intellij.modules.ultimate")) {
                    return Action.NONE
                }
                return super.processError(category, message, details, t)
            }
        }) {
            super.setUp()
        }
        TemplateManagerImpl.setTemplateTesting(testRootDisposable)
        // in unit tests live templates are offered in completion only with this flag
        LiveTemplateCompletionContributor.setShowTemplatesInTests(true, testRootDisposable)
        // completion must not try to start the Lean language server
        service<Lean4Settings>().enableLanguageServer = false
        service<Lean4Settings>().enableLspCompletion = false
        // A second item keeps the lookup open (a single item would be inserted right away); in the IDE the
        // popup also has the LSP items, here a dummy contributor plays that role.
        val ep = CompletionContributorEP(
            "lean4",
            DummyContributor::class.java.name,
            DefaultPluginDescriptor(PluginId.getId("lean4ij.test"), DummyContributor::class.java.classLoader)
        )
        ExtensionPointName<CompletionContributorEP>("com.intellij.completion.contributor").point
            .registerExtension(ep, testRootDisposable)
    }

    class DummyContributor : CompletionContributor() {
        override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
            result.addElement(LookupElementBuilder.create("negligible"))
        }
    }

    override fun tearDown() {
        try {
            service<Lean4Settings>().spaceAfterUnicodeAbbreviation = Lean4Settings.SPACE_ONLY_WITH_SPACE_KEY
            service<Lean4Settings>().enableLanguageServer = true
            service<Lean4Settings>().enableLspCompletion = true
        } finally {
            super.tearDown()
        }
    }

    fun testSpaceAddsSpaceInDefaultMode() {
        myFixture.configureByText("a.lean", "example : \\neg<caret>")
        myFixture.type(' ')
        myFixture.checkResult("example : ¬ <caret>")
    }

    fun testSpaceDoesNotAddSpaceInNeverMode() {
        service<Lean4Settings>().spaceAfterUnicodeAbbreviation = Lean4Settings.SPACE_NEVER
        myFixture.configureByText("a.lean", "example : \\neg<caret>")
        myFixture.type(' ')
        myFixture.checkResult("example : ¬<caret>")
    }

    fun testSpaceKeepsCaretOfTemplateWithEndInside() {
        myFixture.configureByText("a.lean", "\\{}<caret>")
        myFixture.type(' ')
        myFixture.checkResult("{<caret>} ")
    }

    /** Opens the completion popup for a fully typed `\\neg`, selects the `\\neg` template item and returns the lookup. */
    private fun openLookupForNeg(): LookupImpl {
        myFixture.configureByText("a.lean", "example : \\neg<caret>")
        myFixture.completeBasic()
        val lookup = myFixture.lookup as LookupImpl
        val item = lookup.items.first { it is LiveTemplateLookupElementImpl && it.template.key == "\\neg" }
        lookup.currentItem = item
        return lookup
    }

    fun testTabInLookupInsertsWithoutSpace() {
        openLookupForNeg()
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR)
        myFixture.checkResult("example : ¬<caret>")
    }

    fun testEnterInLookupInsertsWithoutSpace() {
        openLookupForNeg()
        myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
        myFixture.checkResult("example : ¬<caret>")
    }

    /** With the lookup open the Space goes through the char filters (Lean4AbbreviationCharFilter). */
    fun testSpaceWithSelectedItemInsertsWithSpace() {
        openLookupForNeg()
        myFixture.type(' ')
        myFixture.checkResult("example : ¬ <caret>")
    }

    fun testSpaceWithSelectedItemInsertsWithoutSpaceInNeverMode() {
        service<Lean4Settings>().spaceAfterUnicodeAbbreviation = Lean4Settings.SPACE_NEVER
        openLookupForNeg()
        myFixture.type(' ')
        myFixture.checkResult("example : ¬<caret>")
    }
}
