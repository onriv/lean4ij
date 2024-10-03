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
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
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
import java.util.function.Supplier
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
    override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
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
class Lean4Settings : PersistentStateComponent<Lean4Settings> {
    var enableNativeInfoview = true
    var enableVscodeInfoview = true
    var enableExtraCssForVscodeInfoview = false
    // TODO this function should not be ref here, but move it to here and ref it from the infoview package maybe
    var extraCssForVscodeInfoview = createThemeCss(EditorColorsManager.getInstance().globalScheme)

    override fun getState() = this

    override fun loadState(state: Lean4Settings) {
        XmlSerializerUtil.copyBean(state, this)
    }
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

class Lean4SettingsView {
    private val lean4Settings = service<Lean4Settings>()

    // Infoview settings
    private val enableNativeInfoview = JBCheckBox("Enable the native infoview", lean4Settings.enableNativeInfoview)
    private val enableVscodeInfoview = JBCheckBox("Enable the vscode infoview", lean4Settings.enableVscodeInfoview)
    private val enableExtraCssForVscodeInfoview = JBCheckBox("Enable extra css for vscode infoview", lean4Settings.enableExtraCssForVscodeInfoview)

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
            extraCssForVscodeInfoview.background = EditorColorsManager.getInstance().globalScheme.getAttributes(DefaultLanguageHighlighterColors.LINE_COMMENT).backgroundColor
        } else {
            extraCssForVscodeInfoview.background = EditorColorsManager.getInstance().globalScheme.getAttributes(HighlighterColors.TEXT).backgroundColor
        }
    }

    val isModified: Boolean get() {
        return enableNativeInfoview.isSelected != lean4Settings.enableNativeInfoview ||
                enableVscodeInfoview.isSelected != lean4Settings.enableVscodeInfoview ||
                enableExtraCssForVscodeInfoview.isSelected != lean4Settings.enableExtraCssForVscodeInfoview ||
                extraCssForVscodeInfoview.text != lean4Settings.extraCssForVscodeInfoview
    }

    fun apply() {
        lean4Settings.enableNativeInfoview = enableNativeInfoview.isSelected
        lean4Settings.enableVscodeInfoview = enableVscodeInfoview.isSelected
        lean4Settings.enableExtraCssForVscodeInfoview = enableExtraCssForVscodeInfoview.isSelected
        lean4Settings.extraCssForVscodeInfoview = extraCssForVscodeInfoview.text
        // TODO is it OK here runBlocking?
        runBlocking {
            _events.emit(Lean4SettingsState(
                lean4Settings.enableNativeInfoview,
                lean4Settings.enableVscodeInfoview,
                lean4Settings.enableExtraCssForVscodeInfoview,
                lean4Settings.extraCssForVscodeInfoview,
            ))
        }
    }

    fun reset() {
        enableNativeInfoview.isSelected = lean4Settings.enableNativeInfoview
        enableVscodeInfoview.isSelected = lean4Settings.enableVscodeInfoview
        enableExtraCssForVscodeInfoview.isSelected = lean4Settings.enableExtraCssForVscodeInfoview
        extraCssForVscodeInfoview.text = lean4Settings.extraCssForVscodeInfoview
    }

    fun createComponent() = panel {
        group("Infoview Settings") {
            row { cell(enableNativeInfoview) }
            row { cell(enableVscodeInfoview) }
            row { cell(enableExtraCssForVscodeInfoview)}
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

/**
 * The class is mainly from [com.intellij.application.options.colors.ColorAndFontOptions.ConsoleFontConfigurableFactory]
 */
class BrowserInfoviewColorAndFontPanelFactory : ColorAndFontPanelFactory {
    override fun createPanel(options: ColorAndFontOptions): NewColorAndFontPanel {
        val previewPanel: FontEditorPreview = object : FontEditorPreview(Supplier {options.selectedScheme }, true) {
            // override fun updateOptionsScheme(selectedScheme: EditorColorsScheme): EditorColorsScheme {
            //     println("TODO")
            //     return selectedScheme
            // }
        }
        return object : NewColorAndFontPanel(SchemesPanel(options), BrowserInfoviewFontOptions(options), previewPanel, "BrowserInfoviewColorAndFontPanelFactoryTODO", null, null) {
            override fun containsFontOptions(): Boolean {
                return true
            }
        }
    }

    override fun getPanelDisplayName(): String {
        return "Lean4 Browser Infoview Font"
    }
}

/**
 * This class is mainly from [com.intellij.application.options.colors.ConsoleFontOptions]
 */
class BrowserInfoviewFontOptions(options: ColorAndFontOptions) : FontOptions(options) {

    override fun getOverwriteFontTitle(): String {
        return "Use Browser infoview font instead of the"
    }

}