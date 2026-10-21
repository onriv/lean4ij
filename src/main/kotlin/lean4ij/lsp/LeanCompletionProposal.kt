package lean4ij.lsp

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.client.features.LSPCompletionFeature
import com.redhat.devtools.lsp4ij.client.features.LSPCompletionProposal
import lean4ij.project.LeanProjectService
import org.eclipse.lsp4j.CompletionItem
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * [LSPCompletionProposal] that never waits for `completionItem/resolve` on the EDT.
 *
 * LSP4IJ's [LSPCompletionProposal.getExpensiveRenderer] blocks on the resolve future (`awaitWithCheckCanceled`)
 * before returning the renderer. The method is called on the EDT from `LookupImpl.refreshUi` on every typed
 * character while the lookup is open. The Lean server answers `completionItem/resolve` only once the file is
 * elaborated up to the completion position, and each keystroke restarts that elaboration, so the IDE froze for
 * as long as the elaboration took (see notes/2026-10-11-completion-resolve-edt-freeze.md).
 *
 * Here [getExpensiveRenderer] returns immediately, and the waiting happens inside the returned renderer, which
 * the platform runs in a background, cancellable read action (`AsyncRendering`).
 */
class LeanCompletionProposal(
    item: CompletionItem,
    context: LSPCompletionFeature.LSPCompletionContext,
    feature: LSPCompletionFeature,
    private val project: Project,
    private val editor: Editor,
) : LSPCompletionProposal(item, context, feature) {

    @Volatile
    private var resolveFuture: CompletableFuture<CompletionItem>? = null

    override fun getExpensiveRenderer(): LookupElementRenderer<out LookupElement>? {
        // Mirrors the base class: only the selected item is resolved, and only when the server has not
        // already provided a detail. Everything here must be cheap, it runs on the EDT.
        if (!isSelectedItem() || !needToResolveCompletionDetail()) {
            return null
        }
        return object : LookupElementRenderer<LookupElement>() {
            override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
                val resolved = awaitResolved()
                val detail = resolved?.detail
                if (detail != null && item.detail == null) {
                    item.detail = detail
                }
                this@LeanCompletionProposal.renderElement(presentation)
            }
        }
    }

    private fun isSelectedItem(): Boolean {
        // The lookup wraps proposals in LookupElementDecorator, so compare through getObject() like LSP4IJ does.
        val lookup = LookupManager.getActiveLookup(editor) ?: return false
        return lookup.currentItem?.getObject() === this
    }

    /**
     * Runs in the background renderer. Blocks with `checkCanceled`, so a write action (typing) cancels the
     * read action, and the next rendering reuses the same pending future.
     */
    private fun awaitResolved(): CompletionItem? {
        val future = resolveFuture ?: startResolve()?.also { resolveFuture = it } ?: return null
        return try {
            ProgressIndicatorUtils.awaitWithCheckCanceled(future)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: CancellationException) {
            null
        } catch (e: CompletionException) {
            thisLogger().debug("completionItem/resolve failed", e.cause ?: e)
            null
        } catch (e: Exception) {
            thisLogger().debug("completionItem/resolve failed", e)
            null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startResolve(): CompletableFuture<CompletionItem>? {
        val deferred = project.service<LeanProjectService>().languageServer
        if (!deferred.isCompleted) {
            return null
        }
        val server = deferred.getCompleted().languageServer
        return server.textDocumentService.resolveCompletionItem(item)
    }
}
