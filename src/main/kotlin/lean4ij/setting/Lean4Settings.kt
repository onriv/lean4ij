package lean4ij.setting

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
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

    var enableHeuristicTactic = true
    var enableHeuristicField = true
    var enableHeuristicAttributes = true
    var enableHeuristicDefinition = true

    /**
     * TODO add project level configuration for this
     */
    var enableLanguageServer = true
    var enableLeanServerLog = false
    var enableFileProgressBar = true

    var maxInlayHintWaitingMillis = 100
    var commentPrefixForGoalHint = "---"

    var enableDiagnosticsLens = true
    var enableLspCompletion = true

    var enableNativeInfoview = true
    var autoUpdateInternalInfoview = true
    var hoveringTimeBeforePopupNativeInfoviewDoc = 200
    var nativeInfoviewPopupMinWidthTextLengthUpperBound = 0
    var nativeInfoviewPopupMaxWidthTextLengthLowerBound = 100
    var nativeInfoviewPopupPreferredMinWidth = 100
    var nativeInfoviewPopupPreferredMaxWidth = 600

    var enableVscodeInfoview = true

    // TODO this in fact can be different to implement the immutable state directly rather than using an
    //      extra class
    override fun getState() = this

    override fun loadState(state: Lean4Settings) {
        XmlSerializerUtil.copyBean(state, this)
        updateNonPersistent()
    }

    fun updateNonPersistent() {
        nonPersistent().updateFromSetting(this)
    }

    fun nonPersistent() = service<Lean4NonPersistentSetting>()
}

/**
 * This is for settings that in fact is converted from other settings, for keeping [Lean4Settings] in a simple POJO we add a new service here
 */
@Service
class Lean4NonPersistentSetting {
    var commentPrefixForGoalHintRegex : Regex? = null

    fun updateFromSetting(setting: Lean4Settings) {
        commentPrefixForGoalHintRegex = Regex("""(\n\s*${Regex.escape(setting.commentPrefixForGoalHint)})\s*?\n\s*\S""")
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
        boolean("Enable heuristic definition highlighting", settings::enableHeuristicDefinition)
        boolean("Enable heuristic tactic highlighting", settings::enableHeuristicTactic)
        boolean("Enable heuristic field highlighting", settings::enableHeuristicField)
        boolean("Enable heuristic attributes highlighting", settings::enableHeuristicAttributes)
    }
    group("Inlay Hints Settings ") {
        boolean("Enable diagnostics lens for #check, #print, etc (restart to take effect)", settings::enableDiagnosticsLens)
        string("Comment prefix for goal hints", settings::commentPrefixForGoalHint)
        int("Max inlay hint waiting millis", settings::maxInlayHintWaitingMillis, 0, 500, 25)
    }
    group("Language Server Settings") {
        boolean("Enable language server", settings::enableLanguageServer)
        boolean("Enable the lean language server log (restart to take effect)", settings::enableLeanServerLog) {
            comment("<a href='https://github.com/leanprover/lean4/tree/master/src/Lean/Server#in-general'>ref</a>")
        }
        boolean("Enable lsp completion", settings::enableLspCompletion)
    }
    group("Infoview Settings") {
        val (row, component) = boolean("Enable the native infoview", settings::enableNativeInfoview)
        boolean("Auto Update internal infoview", settings::autoUpdateInternalInfoview).let {
            val (row, _) = it
            row.enabledIf(component.selected)
        }
        int("Time limit for popping up native infoview doc (millis): ", settings::hoveringTimeBeforePopupNativeInfoviewDoc, 50, 3000).enabledIf(component.selected)
        int("text length upper bound for using min width", settings::nativeInfoviewPopupMinWidthTextLengthUpperBound, 0, 3000).enabledIf(component.selected)
        int("text length lower bound for using max width", settings::nativeInfoviewPopupMaxWidthTextLengthLowerBound, 0, 3000).enabledIf(component.selected)
        int("native infoview min width", settings::nativeInfoviewPopupPreferredMinWidth, 0, 3000).enabledIf(component.selected)
        int("native infoview max width", settings::nativeInfoviewPopupPreferredMaxWidth, 0, 3000).enabledIf(component.selected)
        boolean("Enable the vscode infoview", settings::enableVscodeInfoview)
    }
}
