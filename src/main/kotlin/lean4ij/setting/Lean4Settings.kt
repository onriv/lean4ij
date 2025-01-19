package lean4ij.setting

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.xmlb.annotations.OptionTag

class CommentPrefixForGoalHint(val pattern: String) {
    companion object {
        fun quote(value: String): String {
            return """(\n\s*${Regex.escape(value)})\s*?\n\s*\S"""
        }
    }
    val regex = Regex(quote(pattern))
    class Converter: com.intellij.util.xmlb.Converter<Regex>() {
        override fun fromString(value: String): Regex? {
            TODO("Not yet implemented")
        }

        override fun toString(value: Regex): String? {
            TODO("Not yet implemented")
        }
    }
}

/**
 * ref: https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html#implementing-the-state-class
 * in our case it seems we don't actually need a delegated state class, but jetbrains doc recommends to use it
 * TODO but it make the setting class [Lean4Settings] a little cumbersome to use now: rather than writing things like `settings.someOption`
 *      we now have to write `settings.state.someOption`. Maybe later we can add some meta programming to make it more convenient, like
 *      dynamic delegate or something
 */
class Lean4State : BaseState() {
    var commentEmptyLine = true
    var commentAtFirstColumn = false
    var useSpaceAfterLineComment = true
    var enableHeuristicTactic = true
    var enableHeuristicField = true
    var enableHeuristicAttributes = true
    var enableHeuristicDefinition = true
    var enableHoverHighlight = true

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

    /**
     * This is not guaranteed not null for it raised exception in buildSearchableOptions when buildPlugin
     * It may be caused by deserialization
     * TODO still the concrete reason should be found out
     */
    @OptionTag(converter = CommentPrefixForGoalHint.Converter::class)
    var commentPrefixForGoalHintRegex : CommentPrefixForGoalHint? = CommentPrefixForGoalHint("---")

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
}

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
class Lean4Settings : SimplePersistentStateComponent<Lean4State>(Lean4State()) {
    override fun loadState(state: Lean4State) {
        super.loadState(state)
    }
}

/**
 * for adding new setting, create a new field in [Lean4Settings] and add an ui for it here
 * for non-persistent typed setting, update [Lean4NonPersistentSetting] too.
 * the function is put here to add setting more convenient
 */
fun Lean4SettingsView.createComponent(settings: Lean4Settings) = panel {
    group("General Settings") {
        boolean("Enable the vertical file progress bar on the left of editor", settings.state::enableFileProgressBar)
        boolean("Enable hover highlight for current term", settings.state::enableHoverHighlight)
        boolean("Enable heuristic definition highlighting", settings.state::enableHeuristicDefinition)
        boolean("Enable heuristic tactic highlighting", settings.state::enableHeuristicTactic)
        boolean("Enable heuristic field highlighting", settings.state::enableHeuristicField)
        boolean("Enable heuristic attributes highlighting", settings.state::enableHeuristicAttributes)
        boolean("Comment at first column", settings.state::commentAtFirstColumn)
        boolean("Use space after line comment", settings.state::useSpaceAfterLineComment)
        boolean("Comment empty line", settings.state::commentEmptyLine)
    }
    group("Inlay Hints Settings ") {
        boolean(
            "Enable diagnostics lens for #check, #print, etc (restart to take effect)",
            settings.state::enableDiagnosticsLens
        )
        string("Comment prefix for goal hints",
            { settings.state.commentPrefixForGoalHintRegex = CommentPrefixForGoalHint(it) },
            // TODO here the pattern should only nullable in buildSearchableOptions when
            //      buildPlugin
            { settings.state.commentPrefixForGoalHintRegex?.pattern?:"---" }
        )
        int("Max inlay hint waiting millis(multiple of 25)", settings.state::maxInlayHintWaitingMillis, 0, 500, 25)
    }
    group("Language Server Settings") {
        boolean("Enable language server", settings.state::enableLanguageServer)
        select(
            "Language server starting strategy",
            arrayOf(
                "Eager",
                "Lazy",
            ),
            settings.state::languageServerStartingStrategy,
            listOf(
                "Eagerly start the language server at opening project",
                "Lazily start the language server until focusing the file"
            )
        )
        select(
            "File processing triggering strategy",
            arrayOf(
                "OnlySelectedEditor",
                "AllOpenedEditor",
            ),
            settings.state::fileProgressTriggeringStrategy,
            listOf(
                "Trigger file progressing for only the selected editor while opening project",
                "Trigger file progressing for only all opened editor while opening project"
            )
        )
        boolean("Enable the lean language server log (restart to take effect)", settings.state::enableLeanServerLog) {
            comment("<a href='https://github.com/leanperrover/lean4/tree/master/src/Lean/Server#in-general'>ref</a>")
        }
        val workspaceSymbolsOrClassesRequestsStrategy = select(
            "Strategy for triggering workspace symbols/classes request",
            arrayOf("debounce", "suffix"),
            settings.state::strategyForTriggeringSymbolsOrClassesRequests,
            listOf(
                "use debounce, request is triggering after idle for configured time",
                "use suffix string, request is trigger after certain suffix string entered (and delete them for the final result)"
            )
        )
        string(
            "Suffix string for triggering workspace symbol/class request",
            settings.state::workspaceSymbolTriggerSuffix
        )
            .enabledIf(workspaceSymbolsOrClassesRequestsStrategy.isSelecting("suffix"))
        int(
            "Debouncing time for triggering workspace symbol/class request",
            settings.state::workspaceSymbolTriggerDebouncingTime,
            200,
            3000
        )
            .enabledIf(workspaceSymbolsOrClassesRequestsStrategy.isSelecting("debounce"))
        boolean("Enable lsp completion", settings.state::enableLspCompletion)
    }
    group("Infoview Settings") {
        select(
            "Select preferred infoview",
            arrayOf(
                "Jcef",
                "Swing",
            ),
            settings.state::preferredInfoview,
            listOf("Prefer the Jcef/External/Vscode infoview", "Prefer the Swing/Native/Internal infoview")
        )

        val enableNativeInfoview = boolean("Enable the native infoview", settings.state::enableNativeInfoview)
        boolean("Auto Update internal infoview", settings.state::autoUpdateInternalInfoview).enabledIf(
            enableNativeInfoview.selected
        )
        int(
            "Time limit for popping up native infoview doc (millis).state: ",
            settings.state::hoveringTimeBeforePopupNativeInfoviewDoc,
            50,
            3000
        ).enabledIf(enableNativeInfoview.selected)
        int(
            "text length upper bound for using min width",
            settings.state::nativeInfoviewPopupMinWidthTextLengthUpperBound,
            0,
            3000
        ).enabledIf(enableNativeInfoview.selected)
        int(
            "text length lower bound for using max width",
            settings.state::nativeInfoviewPopupMaxWidthTextLengthLowerBound,
            0,
            3000
        ).enabledIf(enableNativeInfoview.selected)
        int("native infoview min width", settings.state::nativeInfoviewPopupPreferredMinWidth, 0, 3000).enabledIf(
            enableNativeInfoview.selected
        )
        int("native infoview max width", settings.state::nativeInfoviewPopupPreferredMaxWidth, 0, 3000).enabledIf(
            enableNativeInfoview.selected
        )
        boolean("Enable the vscode infoview", settings.state::enableVscodeInfoview)
    }
}
