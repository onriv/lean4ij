package lean4ij

import com.intellij.application.options.colors.ColorAndFontOptions
import com.intellij.application.options.colors.ColorAndFontPanelFactory
import com.intellij.application.options.colors.FontEditorPreview
import com.intellij.application.options.colors.FontOptions
import com.intellij.application.options.colors.NewColorAndFontPanel
import com.intellij.application.options.colors.SchemesPanel
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.xmlb.XmlSerializerUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import lean4ij.infoview.external.createThemeCss
import java.awt.Component
import javax.swing.AbstractButton
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JList


// UI DSL
fun Panel.labeled(text: String, component: JComponent) = row {
    cell(component).label(text)
}

fun <T : JComponent> Panel.aligned(text: String, component: T, init: Cell<T>.() -> Unit = {}) = row(text) {
    cell(component).align(AlignX.FILL).init()
}

fun <T : JComponent> Panel.checked(checkBox: AbstractButton, component: T, init: Cell<T>.() -> Unit = {}) = row {
    cell(checkBox)
    cell(component).enabledIf(checkBox.selected).init()
}


class ToolTipListCellRenderer(private val toolTips: List<String>) : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (index >= 0 && index < toolTips.size) {
            list.toolTipText = toolTips[index]
        }
        return comp
    }
}

@State(
    name = "Lean4Settings",
    storages = [Storage("Lean4.xml")]
)
// TODO this in fact can be different to implement the immutable state directly rather than using an
//      extra class
class Lean4Settings : PersistentStateComponent<Lean4Settings> {

    var enableFileProgressBar = true

    var commentPrefixForGoalHint : String = "---"
    var commentPrefixForGoalHintRegex = updateCommentPrefixForGoalHintRegex()
    var enableDiagnosticsLens = true
    var enableLspCompletion = true

    var enableNativeInfoview = true
    var hoveringTimeBeforePopupNativeInfoviewDoc = 200
    var nativeInfoviewPopupMinWidthTextLengthUpperBound = 200
    var nativeInfoviewPopupMaxWidthTextLengthLowerBound = 1000
    var nativeInfoviewPopupPreferredMinWidth = 500
    var nativeInfoviewPopupPreferredMaxWidth = 800

    var disableNativeInfoviewUpdateAtWindowClosed = false
    var enableVscodeInfoview = true
    var disableVscodeInfoviewUpdateAtWindowClosed = false
    var enableExtraCssForVscodeInfoview = false

    // TODO this function should not be ref here, but move it to here and ref it from the infoview package maybe
    var extraCssForVscodeInfoview = createThemeCss(EditorColorsManager.getInstance().globalScheme)

    // TODO this in fact can be different to implement the immutable state directly rather than using an
    //      extra class
    override fun getState() = this

    override fun loadState(state: Lean4Settings) {
        XmlSerializerUtil.copyBean(state, this)
        commentPrefixForGoalHintRegex = updateCommentPrefixForGoalHintRegex()
    }

    private fun updateCommentPrefixForGoalHintRegex() =
        Regex("""(\n\s*${Regex.escape(commentPrefixForGoalHint)})\s*?\n\s*\S""")
}

/**
 * Here the fields are the same as [Lean4Settings] but immutable
 */
data class Lean4SettingsState(
    val enableNativeInfoview: Boolean,
    val enableVscodeInfoview: Boolean,
    val enableExtraCssForVscodeInfoview: Boolean,
    val extraCssForVscodeInfoview: String,
)

/**
 * TODO adding setting is cumbersome, check if it's any better way to do it
 */
class Lean4SettingsView {
    private val lean4Settings = service<Lean4Settings>()
    
    private val commentPrefixForGoalHint = JBTextField(lean4Settings.commentPrefixForGoalHint)
    private val enableDiagnosticLens = JBCheckBox("Enable diagnostics lens for #check, #print, etc (restart to take effect)", lean4Settings.enableDiagnosticsLens)
    private val enableFileProgressBar = JBCheckBox("Enable the vertical file progress bar on the left of editor", lean4Settings.enableFileProgressBar)

    private val enableLspCompletion = JBCheckBox("Enable lsp completion", lean4Settings.enableLspCompletion)

    // Infoview settings
    private val enableNativeInfoview = JBCheckBox("Enable the native infoview", lean4Settings.enableNativeInfoview)
    private val hoveringTimeBeforePopupNativeInfoviewDoc =
        JBIntSpinner(lean4Settings.hoveringTimeBeforePopupNativeInfoviewDoc, 50, 3000)
    private val nativeInfoviewPopupMinWidthTextLengthUpperBound =
        JBIntSpinner(lean4Settings.nativeInfoviewPopupMinWidthTextLengthUpperBound, 0, 3000)
    private val nativeInfoviewPopupMaxWidthTextLengthLowerBound =
        JBIntSpinner(lean4Settings.nativeInfoviewPopupMaxWidthTextLengthLowerBound, 0, 3000)
    private val nativeInfoviewPopupPreferredMinWidth =
        JBIntSpinner(lean4Settings.nativeInfoviewPopupPreferredMinWidth, 0, 3000)
    private val nativeInfoviewPopupPreferredMaxWidth =
        JBIntSpinner(lean4Settings.nativeInfoviewPopupPreferredMaxWidth, 0, 3000)

    // TODO
    private val disableNativeInfoviewUpdateAtWindowClosed = JBCheckBox(
        "Disable native infoview update at tool window closed",
        lean4Settings.disableNativeInfoviewUpdateAtWindowClosed
    )

    // TODO
    private val enableVscodeInfoview = JBCheckBox("Enable the vscode infoview", lean4Settings.enableVscodeInfoview)

    // TODO
    private val disableVscodeInfoviewUpdateAtWindowClosed = JBCheckBox(
        "Disable vscode infoview update at tool window closed",
        lean4Settings.disableVscodeInfoviewUpdateAtWindowClosed
    )
    private val enableExtraCssForVscodeInfoview =
        JBCheckBox("Enable extra css for vscode infoview", lean4Settings.enableExtraCssForVscodeInfoview)

    /**
     * copy from [com.intellij.ui.dsl.builder.impl.RowImpl.textArea]
     */
    private val extraCssForVscodeInfoview = run {
        val textArea = JBTextArea()
        // Text area should have same margins as TextField. When margin is TestArea used then border is MarginBorder and margins are taken
        // into account twice, which is hard to workaround in current API. So use border instead
        textArea.border = JBEmptyBorder(3, 5, 3, 5)
        textArea.columns = COLUMNS_SHORT
        textArea.font = JBFont.regular()
        // Text area should have same margins as TextField. When margin is TestArea used then border is MarginBorder and margins are taken
        // into account twice, which is hard to workaround in current API. So use border instead
        textArea
    }

    init {
        setExtraCssForTextAreaIsEditable(lean4Settings.enableExtraCssForVscodeInfoview)
        extraCssForVscodeInfoview.text = lean4Settings.extraCssForVscodeInfoview
        enableExtraCssForVscodeInfoview.selected.addListener {
            setExtraCssForTextAreaIsEditable(it)
        }
    }

    private fun setExtraCssForTextAreaIsEditable(isEditable: Boolean) {
        extraCssForVscodeInfoview.isEditable = isEditable
        if (!isEditable) {
            extraCssForVscodeInfoview.background =
                EditorColorsManager.getInstance().globalScheme.getAttributes(DefaultLanguageHighlighterColors.LINE_COMMENT).backgroundColor
        } else {
            extraCssForVscodeInfoview.background =
                EditorColorsManager.getInstance().globalScheme.getAttributes(HighlighterColors.TEXT).backgroundColor
        }
    }

    /**
     * TODO one setting, 5 positions must be changed... [Lean4Settings], [Lean4SettingsView], here [isModified], [apply], [reset],
     *      there must be some more clean way with declaration style
     */
    val isModified: Boolean
        get() {

            val commentPrefixForGoalHintChanged = commentPrefixForGoalHint.text != lean4Settings.commentPrefixForGoalHint

            val enableNativeInfoviewChanged = enableNativeInfoview.isSelected != lean4Settings.enableNativeInfoview
            val enableVscodeInfoviewChanged = enableVscodeInfoview.isSelected != lean4Settings.enableVscodeInfoview
            val enableExtraCssForVscodeInfoviewChanged =
                enableExtraCssForVscodeInfoview.isSelected != lean4Settings.enableExtraCssForVscodeInfoview
            val extraCssForVscodeInfoviewChanged =
                extraCssForVscodeInfoview.text != lean4Settings.extraCssForVscodeInfoview
            val hoveringTimeBeforePopupNativeInfoviewDocChanged =
                hoveringTimeBeforePopupNativeInfoviewDoc.number != lean4Settings.hoveringTimeBeforePopupNativeInfoviewDoc
            val nativeInfoviewPopupTextWidth1Changed =
                nativeInfoviewPopupMinWidthTextLengthUpperBound.number != lean4Settings.nativeInfoviewPopupMinWidthTextLengthUpperBound
            val nativeInfoviewPopupTextWidth2Changed =
                nativeInfoviewPopupMaxWidthTextLengthLowerBound.number != lean4Settings.nativeInfoviewPopupMaxWidthTextLengthLowerBound
            val nativeInfoviewPopupPreferredMinWidthChanged =
                nativeInfoviewPopupPreferredMinWidth.number != lean4Settings.nativeInfoviewPopupPreferredMinWidth
            val nativeInfoviewPopupPreferredMaxWidthChanged =
                nativeInfoviewPopupPreferredMaxWidth.number != lean4Settings.nativeInfoviewPopupPreferredMaxWidth
            val enableLspCompletionChanged = enableLspCompletion.isSelected != lean4Settings.enableLspCompletion
            val enableDiagnosticLensChanged = enableDiagnosticLens.isSelected != lean4Settings.enableDiagnosticsLens
            val enableFileProgressBarChanged = enableFileProgressBar.isSelected != lean4Settings.enableFileProgressBar
            return enableNativeInfoviewChanged || enableVscodeInfoviewChanged || enableExtraCssForVscodeInfoviewChanged ||
                    extraCssForVscodeInfoviewChanged || hoveringTimeBeforePopupNativeInfoviewDocChanged || enableLspCompletionChanged ||
                    nativeInfoviewPopupTextWidth1Changed || nativeInfoviewPopupTextWidth2Changed ||
                    nativeInfoviewPopupPreferredMinWidthChanged || nativeInfoviewPopupPreferredMaxWidthChanged
                    || commentPrefixForGoalHintChanged
                    || enableDiagnosticLensChanged
                    || enableFileProgressBarChanged
        }

    fun apply() {
        lean4Settings.commentPrefixForGoalHint = commentPrefixForGoalHint.text
        lean4Settings.commentPrefixForGoalHintRegex = Regex("""(\n\s*${lean4Settings.commentPrefixForGoalHint})\s*?\n\s*\S""")
        lean4Settings.enableDiagnosticsLens = enableDiagnosticLens.isSelected

        lean4Settings.enableNativeInfoview = enableNativeInfoview.isSelected
        lean4Settings.enableVscodeInfoview = enableVscodeInfoview.isSelected
        lean4Settings.enableExtraCssForVscodeInfoview = enableExtraCssForVscodeInfoview.isSelected
        lean4Settings.extraCssForVscodeInfoview = extraCssForVscodeInfoview.text
        lean4Settings.hoveringTimeBeforePopupNativeInfoviewDoc = hoveringTimeBeforePopupNativeInfoviewDoc.number
        lean4Settings.nativeInfoviewPopupMinWidthTextLengthUpperBound = nativeInfoviewPopupMinWidthTextLengthUpperBound.number
        lean4Settings.nativeInfoviewPopupMaxWidthTextLengthLowerBound = nativeInfoviewPopupMaxWidthTextLengthLowerBound.number
        lean4Settings.nativeInfoviewPopupPreferredMinWidth = nativeInfoviewPopupPreferredMinWidth.number
        lean4Settings.nativeInfoviewPopupPreferredMaxWidth = nativeInfoviewPopupPreferredMaxWidth.number
        lean4Settings.enableLspCompletion = enableLspCompletion.isSelected
        lean4Settings.enableFileProgressBar = enableFileProgressBar.isSelected
        // TODO is it OK here runBlocking?
        // TODO full state
        runBlocking {
            _events.emit(
                Lean4SettingsState(
                    lean4Settings.enableNativeInfoview,
                    lean4Settings.enableVscodeInfoview,
                    lean4Settings.enableExtraCssForVscodeInfoview,
                    lean4Settings.extraCssForVscodeInfoview,
                )
            )
        }
    }

    fun reset() {
        enableDiagnosticLens.isSelected = lean4Settings.enableDiagnosticsLens
        commentPrefixForGoalHint.text = lean4Settings.commentPrefixForGoalHint
        enableNativeInfoview.isSelected = lean4Settings.enableNativeInfoview
        enableVscodeInfoview.isSelected = lean4Settings.enableVscodeInfoview
        enableExtraCssForVscodeInfoview.isSelected = lean4Settings.enableExtraCssForVscodeInfoview
        extraCssForVscodeInfoview.text = lean4Settings.extraCssForVscodeInfoview
        hoveringTimeBeforePopupNativeInfoviewDoc.number = lean4Settings.hoveringTimeBeforePopupNativeInfoviewDoc
        nativeInfoviewPopupMinWidthTextLengthUpperBound.number = lean4Settings.nativeInfoviewPopupMinWidthTextLengthUpperBound
        nativeInfoviewPopupMaxWidthTextLengthLowerBound.number = lean4Settings.nativeInfoviewPopupMaxWidthTextLengthLowerBound
        nativeInfoviewPopupPreferredMinWidth.number = lean4Settings.nativeInfoviewPopupPreferredMinWidth
        nativeInfoviewPopupPreferredMaxWidth.number = lean4Settings.nativeInfoviewPopupPreferredMaxWidth
        enableLspCompletion.isSelected = lean4Settings.enableLspCompletion
        enableFileProgressBar.isSelected = lean4Settings.enableFileProgressBar
    }

    fun createComponent() = panel {
        group("General Settings") {
            row { cell(enableFileProgressBar) }
        }
        group("Inlay Hints Settings ") {
            row { cell(enableDiagnosticLens) }
            labeled("Comment prefix for goal hints", commentPrefixForGoalHint)
        }
        group("Language Server Settings") {
            row { cell(enableLspCompletion) }
        }
        group("Infoview Settings") {
            row { cell(enableNativeInfoview) }
            labeled(
                "Time limit for popping up native infoview doc (millis): ",
                hoveringTimeBeforePopupNativeInfoviewDoc
            ).enabledIf(enableNativeInfoview.selected)
            labeled(
                "text length upper bound for using min width",
                nativeInfoviewPopupMinWidthTextLengthUpperBound
            ).enabledIf(enableNativeInfoview.selected)
            labeled(
                "text length lower bound for using max width",
                nativeInfoviewPopupMaxWidthTextLengthLowerBound
            ).enabledIf(enableNativeInfoview.selected)
            labeled(
                "native infoview min width",
                nativeInfoviewPopupPreferredMinWidth
            ).enabledIf(enableNativeInfoview.selected)
            labeled(
                "native infoview max width",
                nativeInfoviewPopupPreferredMaxWidth
            ).enabledIf(enableNativeInfoview.selected)
            row { cell(enableVscodeInfoview) }
            row { cell(enableExtraCssForVscodeInfoview) }
            row {
                scrollCell(extraCssForVscodeInfoview).align(AlignX.FILL)
            }
        }
    }

    companion object {
        private val _events = MutableSharedFlow<Lean4SettingsState>()

        // TODO maybe the state also indicates the change detail
        val events: SharedFlow<Lean4SettingsState> = _events.asSharedFlow()
    }

}

class Lean4Configurable : SearchableConfigurable {
    private var settingsView: Lean4SettingsView? = null

    override fun getId() = "preferences.language.Lean4"

    override fun getDisplayName() = "Lean4"

    override fun isModified() = settingsView?.isModified == true

    override fun apply() {
        settingsView?.apply()
    }

    override fun reset() {
        settingsView?.reset()
    }

    override fun createComponent(): JComponent? {
        settingsView = Lean4SettingsView()
        return settingsView?.createComponent()
    }
}