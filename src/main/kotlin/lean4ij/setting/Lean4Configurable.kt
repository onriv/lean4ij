package lean4ij.setting

import com.intellij.openapi.components.service
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.selected
import com.intellij.ui.layout.selectedValueMatches
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JList
import kotlin.reflect.KMutableProperty0

class ToolTipListCellRenderer(private val toolTips: List<String>) : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
        val comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (index >= 0 && index < toolTips.size) {
            list.toolTipText = toolTips[index]
        }
        return comp
    }
}

class BooleanSetting(private val row: Row, private val checkbox: JBCheckBox) {

    fun enabledIf(predicate: ComponentPredicate): BooleanSetting {
        row.enabledIf(predicate)
        return this
    }

    val selected: ComponentPredicate = checkbox.selected

}

class SelectSetting(private val row: Row, private val comboBox: ComboBox<String>) {
    fun enabledIf(predicate: ComponentPredicate) : SelectSetting {
        row.enabledIf(predicate)
        return this
    }

    fun selectedItem(): String = comboBox.selectedItem as String

    fun isSelecting(target: String) : ComponentPredicate = comboBox.selectedValueMatches { it == target }
}

class Lean4SettingsView {
    private val lean4Settings = service<Lean4Settings>()

    val isModified: Boolean
        get() {
            var changed = false
            for (predicate in isChangedPredicates) {
                if (predicate()) {
                    changed = true
                    break
                }
            }
            return changed
        }

    fun apply() {
        for (action in applyActions) {
            action()
        }
    }

    fun reset() {
        for (action in resetActions) {
            action()
        }
    }

    /**
     * The return type is not perfect as a dsl, but currently I cannot get better idea.
     */
    fun Panel.boolean(text: String, prop: KMutableProperty0<Boolean>, others: Row.()->Unit={}): BooleanSetting {
        val component = JBCheckBox(text, prop.get())
        val row = row {
            applyActions.add { prop.set(component.isSelected) }
            resetActions.add { component.isSelected = prop.get() }
            isChangedPredicates.add { component.isSelected != prop.get() }
            cell(component)
            others()
        }
        return BooleanSetting(row, component)
    }

    fun Panel.string(text: String, prop: KMutableProperty0<String>) = row {
        val component = JBTextField(prop.get())
        applyActions.add { prop.set(component.text) }
        resetActions.add { component.text = prop.get() }
        isChangedPredicates.add { component.text != prop.get() }
        cell(component).label(text)
    }

    fun Panel.int(text: String, prop: KMutableProperty0<Int>, minValue: Int, maxValue: Int, stepSize: Int=1) = row {
        val component = JBIntSpinner(prop.get(), minValue, maxValue, stepSize)
        applyActions.add { prop.set(component.number) }
        resetActions.add { component.number = prop.get() }
        isChangedPredicates.add { component.number != prop.get() }
        cell(component).label(text)
    }

    fun Panel.select(text: String, options: Array<String>, prop: KMutableProperty0<String>) = select(text, options, prop, null)

    fun Panel.select(text: String, options: Array<String>, prop: KMutableProperty0<String>, toolTips: List<String>?) : SelectSetting {
        val comboBox = ComboBox(options)
        val row = row {
            if (toolTips?.isNotEmpty() == true) {
                comboBox.apply {
                    renderer = ToolTipListCellRenderer(toolTips)
                }
            }
            cell(comboBox).label(text)
            applyActions.add { prop.set(comboBox.selectedItem as String) }
            resetActions.add { comboBox.selectedItem = prop.get() }
            isChangedPredicates.add { comboBox.selectedItem != prop.get() }
        }
        return SelectSetting(row, comboBox)
    }

    // TODO maybe it should not be public, but currently still no api for combos
    val applyActions : MutableList<()->Unit> = mutableListOf()
    val resetActions : MutableList<()->Unit> = mutableListOf()
    val isChangedPredicates : MutableList<()->Boolean> = mutableListOf()
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
        val lean4Settings = service<Lean4Settings>()
        settingsView = Lean4SettingsView()
        return settingsView?.createComponent(lean4Settings)
    }
}