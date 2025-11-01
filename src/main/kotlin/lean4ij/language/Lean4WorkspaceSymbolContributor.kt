package lean4ij.language

// it is removed
// import com.redhat.devtools.lsp4ij.features.workspaceSymbol.LSPWorkspaceSymbolContributor
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.ExecutionError
import com.google.common.util.concurrent.UncheckedExecutionException
import com.intellij.ide.util.gotoByName.ChooseByNamePopup
import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import com.jetbrains.rd.util.AtomicInteger
import com.redhat.devtools.lsp4ij.LanguageServerManager
import kotlinx.coroutines.FlowPreview
import lean4ij.project.LeanProjectService
import lean4ij.setting.Lean4Settings
import org.eclipse.lsp4j.WorkspaceSymbol
import org.eclipse.lsp4j.WorkspaceSymbolParams
import java.time.Duration
import java.util.concurrent.ExecutionException


/**
 * copy from LSPWorkspaceSymbolContributor
 * for currently in lean4 the api `$/cancelRequest` seems, not cancellable in fact...
 * Hence, we copy the class for do some
 * ref: https://plugins.jetbrains.com/docs/intellij/go-to-class-and-go-to-symbol.html
 * TODO maybe do some PR to Lean4 and move back to LSPWorkspaceSymbolContributor
 * TODO the order seems incorrect and cannot rewrite
 * TODO cannot open result in find tool window, don't know why
 * TODO if possible remove this and move back to lsp4ij
 * TODO remove using the class LeanWorkspaceSymbolData, the involvement
 */
abstract class Lean4ChooseByNameContributorEx : ChooseByNameContributorEx {

    abstract fun filter(data: LeanWorkspaceSymbolData): Boolean

    override fun processNames(
        processor: Processor<in String?>,
        scope: GlobalSearchScope,
        filter: IdFilter?
    ) {
        val project = scope.project ?: return
        // TODO is it better way for this?
        if (!project.service<LeanProjectService>().isLeanProject()) {
            return
        }
        var queryString = project.getUserData(ChooseByNamePopup.CURRENT_SEARCH_PATTERN)
        if (queryString == null) {
            queryString = ""
        }
        val items = getWorkspaceSymbols(queryString, project)
        items?.stream()
            ?.filter { data -> data.file != null && data.file.extension == "lean" }
            ?.filter { data -> filter(data) }
            ?.filter { data: LeanWorkspaceSymbolData -> scope.accept(data.file!!) }
            ?.map { obj: LeanWorkspaceSymbolData -> obj.name }
            ?.forEach { t: String? ->
                processor.process(t)
            }
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem?>,
        parameters: FindSymbolParameters
    ) {
        val items = getWorkspaceSymbols(name, parameters.project)
        items?.stream()
            ?.filter { data -> data.file != null && data.file.extension == "lean" }
            ?.filter { data -> filter(data) }
            ?.filter { ni: LeanWorkspaceSymbolData -> parameters.searchScope.accept(ni.file!!) }
            ?.forEach { t: LeanWorkspaceSymbolData? ->
                processor.process(t)
            }
    }

    private fun getWorkspaceSymbols(key: String, project: Project): List<LeanWorkspaceSymbolData>? {
        val workspaceCache = project.service<WorkspaceSymbolsCache>()
        val items = workspaceCache.getWorkspaceSymbols(key)
        return items
    }

}

class Lean4WorkspaceSymbolContributor : Lean4ChooseByNameContributorEx() {
    override fun filter(data: LeanWorkspaceSymbolData): Boolean {
        return true
    }
}

class Lean4WorkspaceClassContributor : Lean4ChooseByNameContributorEx() {
    override fun filter(data: LeanWorkspaceSymbolData): Boolean {
        return data.name.split(".").last().let { it[0].isUpperCase() }
    }
}

class WorkspaceSymbolsCacheLoader(private val project: Project) :
    CacheLoader<String, List<LeanWorkspaceSymbolData>>() {

    override fun load(key: String): List<LeanWorkspaceSymbolData> {
        thisLogger().info("loading symbols for $key")

        // TODO very fuzzy this way...
        //      after lsp4ij 0.8.0 it can be removed
        //      maybe the best way is make a pr to lean4 for not file progress in didOpen request...
        project.service<LeanProjectService>().isEnable.set(true)
        // TODO change the old way getting language server to this maybe!
        val languageServerItem = LanguageServerManager.getInstance(project)
            .getLanguageServer("lean").get()
        if (languageServerItem == null) {
            // for guava loading cache, in fact this cannot be null
            // return null will trigger an exception and do not cache the value(null)
            // this is the expected behavior
            // we will catch it when using
            throw IllegalStateException("Language server not found")
        }
        val ls = languageServerItem.server
        val params = WorkspaceSymbolParams(key)
        val symbols = ls.workspaceService.symbol(params).get() ?: return listOf()
        val items: MutableList<LeanWorkspaceSymbolData> = ArrayList()
        if (symbols.isLeft) {
            val s = symbols.left
            for (si in s) {
                items.add(LeanWorkspaceSymbolData(si?.name!!, si.kind!!, si.location!!, project))
            }
        } else if (symbols.isRight) {
            val ws = symbols.right
            for (si in ws) {
                val item = createItem(si!!, project)
                items.add(item)
            }
        }
        return items
    }

    fun createItem(si: WorkspaceSymbol, project: Project): LeanWorkspaceSymbolData {
        val name = si.name
        val symbolKind = si.kind
        if (si.location.isLeft) {
            return LeanWorkspaceSymbolData(
                name, symbolKind, si.location.left, project
            )
        }
        return LeanWorkspaceSymbolData(
            name, symbolKind, si.location.right.uri, null, project
        )
    }
}

@OptIn(FlowPreview::class)
@Service(Service.Level.PROJECT)
class WorkspaceSymbolsCache(private val project: Project) {
    private val lean4Settings = service<Lean4Settings>()

    // TODO here every output should also be cache
    private val symbolsCache: LoadingCache<String, List<LeanWorkspaceSymbolData>> = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .build(WorkspaceSymbolsCacheLoader(project))

    private fun canTrigger(queryString: String) = queryString.endsWith(lean4Settings.workspaceSymbolTriggerSuffix)

    private fun normalize(queryString: String) = queryString.removeSuffix(lean4Settings.workspaceSymbolTriggerSuffix)

    fun getWorkspaceSymbolsTriggeredBySuffix(queryString: String): List<LeanWorkspaceSymbolData> {
        if (canTrigger(queryString)) {
            symbolsCache.get(normalize(queryString))
            return listOf()
        }
        // immediately return if the cache contains it
        val data = symbolsCache.getIfPresent(normalize(queryString))
        if (data != null) {
            for (entry in data) {
                symbolsCache.put(entry.name, listOf(entry))
            }
        }
        return data ?: listOf()
    }

    private val requestCounter = AtomicInteger(0)
    private val SLEEP_TIME = 10L

    fun getWorkspaceSymbolsTriggeredByDebounce(queryString: String): List<LeanWorkspaceSymbolData> {
        // immediately return if the cache contains it
        symbolsCache.getIfPresent(queryString)?.let {
            return it
        }
        val currentCnt = requestCounter.incrementAndGet()
        for (i in 1..1000) {
            if (i * SLEEP_TIME > lean4Settings.workspaceSymbolTriggerDebouncingTime) {
                break
            }
            Thread.sleep(SLEEP_TIME)
            val newCnt = requestCounter.get()
            if (currentCnt != newCnt) {
                thisLogger().info("current cnt $currentCnt != new cnt $newCnt for $queryString")
                return listOf()
            }
        }
        try {
            // TODO here it in fact cannot be null
            val symbolDataList = symbolsCache.get(queryString) ?: return listOf()
            for (symbolData in symbolDataList) {
                // put all entries in the symbolsCache, for IJ seems making request to the returned entries too
                symbolData.name.let { symbolsCache.put(it, listOf(symbolData)) }
            }
            return symbolDataList
        } catch (e: Exception) {
            when {
                e is ExecutionException || e is UncheckedExecutionException || e is ExecutionError ->
                    thisLogger().info(e)
                else ->
                    thisLogger().error(e)
            }
            return listOf()
        }
    }

    fun getWorkspaceSymbols(queryString: String): List<LeanWorkspaceSymbolData> {
        if (lean4Settings.strategyForTriggeringSymbolsOrClassesRequests == "suffix") {
            return getWorkspaceSymbolsTriggeredBySuffix(queryString)
        } else {
            return getWorkspaceSymbolsTriggeredByDebounce(queryString)
        }
    }

}