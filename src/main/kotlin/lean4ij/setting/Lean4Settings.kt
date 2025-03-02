package lean4ij.setting

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * TODO this in fact can be different to implement the immutable state directly rather than using an
 *      extra class
 * for adding a new setting, create a new field here, and add it in [Lean4SettingsView.createComponent]
 * for non persistent typed setting, add it to [Lean4NonPersistentSetting] too
 */
@State(
    name = "Lean4Settings",
    storages = [Storage("Lean4.xml")]
)
class Lean4Settings : PersistentStateComponent<Lean4Settings> {
    var commentEmptyLine = true
    var commentAtFirstColumn = false
    var useSpaceAfterLineComment = true
    var enableHeuristicTactic = true
    var enableHeuristicField = true
    var enableHeuristicAttributes = true
    var enableHeuristicDefinition = true
    var enableHoverHighlight = true

    var addSpaceAfterLiveTemplates = true
    var autoCompletePairLiveTemplates = true
    var enableBothSpaceAndNonSpaceLiveTemplates = false

    /**
     * TODO add project level configuration for this
     */
    var enableLanguageServer = true
    var enableLeanServerLog = false
    var enableFileProgressBar = true
    // TODO use constant or enum for this
    //      a reason currently it's not enum is for serialization/persistence
    var languageServerStartingStrategy = "Eager"
    var fileProgressTriggeringStrategy = "OnlySelectedEditor"

    var maxInlayHintWaitingMillis = 100
    var strategyForTriggeringSymbolsOrClassesRequests = "debounce"
    var workspaceSymbolTriggerSuffix = ",,"
    var workspaceSymbolTriggerDebouncingTime = 1000

    // ref: https://kotlinlang.org/docs/delegated-properties.html#observable-properties
    var commentPrefixForGoalHint: String = "---"

    var enableDiagnosticsLens = true
    var enableLspCompletion = true

    // TODO use constant or enum for this
    var preferredInfoview = "Jcef"
    var enableNativeInfoview = true
    var autoUpdateInternalInfoview = true
    var hoveringTimeBeforePopupNativeInfoviewDoc = 200
    var nativeInfoviewPopupMinWidthTextLengthUpperBound = 0
    var nativeInfoviewPopupMaxWidthTextLengthLowerBound = 100
    var nativeInfoviewPopupPreferredMinWidth = 100
    var nativeInfoviewPopupPreferredMaxWidth = 600

    var enableVscodeInfoview = true

    var showAllMessagesInInternalInfoview = true
    var showMessagesInInternalInfoview = true
    var showExpectedTypeInInternalInfoview = true

    // TODO this in fact can be different to implement the immutable state directly rather than using an
    //      extra class
    override fun getState() = this

    override fun loadState(state: Lean4Settings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun getCommentPrefixForGoalHintRegex() : Regex {
        return Regex("""(\n\s*${Regex.escape(commentPrefixForGoalHint)})\s*?\n\s*\S""")
    }
}

/**
 * for adding new setting, create a new field in [Lean4Settings] and add an ui for it here
 * for non-persistent typed setting, update [Lean4NonPersistentSetting] too.
 * the function is put here to add setting more convenient
 */
fun Lean4SettingsView.createComponent(settings: Lean4Settings) = panel {
    group("General Settings") {
        boolean("Enable the vertical file progress bar on the left of editor", settings::enableFileProgressBar)
        boolean("Enable hover highlight for current term", settings::enableHoverHighlight)
        boolean("Enable heuristic definition highlighting", settings::enableHeuristicDefinition)
        boolean("Enable heuristic tactic highlighting", settings::enableHeuristicTactic)
        boolean("Enable heuristic field highlighting", settings::enableHeuristicField)
        boolean("Enable heuristic attributes highlighting", settings::enableHeuristicAttributes)
        boolean("Comment at first column", settings::commentAtFirstColumn)
        boolean("Use space after line comment", settings::useSpaceAfterLineComment)
        boolean("Comment empty line", settings::commentEmptyLine)
        boolean("Add whitespace after live templates for unicode", settings::addSpaceAfterLiveTemplates)
        boolean("Autocomplete live templates for pair unicode", settings::autoCompletePairLiveTemplates)
        boolean("Enable both spaced and non spaced live templates", settings::enableBothSpaceAndNonSpaceLiveTemplates)
    }
    group("Inlay Hints Settings ") {
        boolean("Enable diagnostics lens for #check, #print, etc (restart to take effect)", settings::enableDiagnosticsLens)
        string("Comment prefix for goal hints", settings::commentPrefixForGoalHint)
        int("Max inlay hint waiting millis(multiple of 25)", settings::maxInlayHintWaitingMillis, 0, 500, 25)
    }
    group("Language Server Settings") {
        boolean("Enable language server", settings::enableLanguageServer)
        select(
            "Language server starting strategy",
            arrayOf(
                "Eager",
                "Lazy",
            ),
            settings::languageServerStartingStrategy,
            listOf(
                "Eagerly start the language server at opening project",
                "Lazily start the language server until focusing the file"
            )
        )
        select("File processing triggering strategy",
            arrayOf(
                "OnlySelectedEditor",
                "AllOpenedEditor",
            ),
            settings::fileProgressTriggeringStrategy,
            listOf(
                "Trigger file progressing for only the selected editor while opening project",
                "Trigger file progressing for only all opened editor while opening project"
            )
        )
        boolean("Enable the lean language server log (restart to take effect)", settings::enableLeanServerLog) {
            comment("<a href='https://github.com/leanperrover/lean4/tree/master/src/Lean/Server#in-general'>ref</a>")
        }
        val workspaceSymbolsOrClassesRequestsStrategy = select("Strategy for triggering workspace symbols/classes request",
            arrayOf("debounce", "suffix"),
            settings::strategyForTriggeringSymbolsOrClassesRequests,
            listOf(
                "use debounce, request is triggering after idle for configured time",
                "use suffix string, request is trigger after certain suffix string entered (and delete them for the final result)"
            )
        )
        string("Suffix string for triggering workspace symbol/class request", settings::workspaceSymbolTriggerSuffix)
            .enabledIf(workspaceSymbolsOrClassesRequestsStrategy.isSelecting("suffix"))
        int("Debouncing time for triggering workspace symbol/class request", settings::workspaceSymbolTriggerDebouncingTime, 200, 3000)
            .enabledIf(workspaceSymbolsOrClassesRequestsStrategy.isSelecting("debounce"))
        boolean("Enable lsp completion", settings::enableLspCompletion)
    }
    group("Infoview Settings") {
        select(
            "Select preferred infoview",
            arrayOf(
                "Jcef",
                "Swing",
            ),
            settings::preferredInfoview,
            listOf("Prefer the Jcef/External/Vscode infoview", "Prefer the Swing/Native/Internal infoview")
        )

        val enableNativeInfoview = boolean("Enable the native infoview", settings::enableNativeInfoview)
        boolean("Auto Update internal infoview", settings::autoUpdateInternalInfoview).enabledIf(enableNativeInfoview.selected)
        int("Time limit for popping up native infoview doc (millis): ", settings::hoveringTimeBeforePopupNativeInfoviewDoc, 50, 3000).enabledIf(enableNativeInfoview.selected)
        int("text length upper bound for using min width", settings::nativeInfoviewPopupMinWidthTextLengthUpperBound, 0, 3000).enabledIf(enableNativeInfoview.selected)
        int("text length lower bound for using max width", settings::nativeInfoviewPopupMaxWidthTextLengthLowerBound, 0, 3000).enabledIf(enableNativeInfoview.selected)
        int("native infoview min width", settings::nativeInfoviewPopupPreferredMinWidth, 0, 3000).enabledIf(enableNativeInfoview.selected)
        int("native infoview max width", settings::nativeInfoviewPopupPreferredMaxWidth, 0, 3000).enabledIf(enableNativeInfoview.selected)
        boolean("Enable the vscode infoview", settings::enableVscodeInfoview)
        boolean("Show All messages in internal infoview", settings::showAllMessagesInInternalInfoview)
        boolean("Show message in internal infoview", settings::showMessagesInInternalInfoview)
        boolean("Show expected type in internal infoview", settings::showExpectedTypeInInternalInfoview)
    }
}
