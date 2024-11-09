package lean4ij.setting

import com.intellij.openapi.components.service
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import javax.swing.JComponent
import kotlin.reflect.KMutableProperty0


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
        lean4Settings.updateNonPersistent()
    }

    fun reset() {
        for (action in resetActions) {
            action()
        }
    }

    /**
     * The return type is not perfect as a dsl, but currently I cannot get better idea.
     */
    fun Panel.boolean(text: String, prop: KMutableProperty0<Boolean>, others: Row.()->Unit={}): Pair<Row, JBCheckBox> {
        val component = JBCheckBox(text, prop.get())
        val row = row {
            applyActions.add { prop.set(component.isSelected) }
            resetActions.add { component.isSelected = prop.get() }
            isChangedPredicates.add { component.isSelected != prop.get() }
            cell(component)
            others()
        }
        return Pair(row, component)
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

    private val applyActions : MutableList<()->Unit> = mutableListOf()
    private val resetActions : MutableList<()->Unit> = mutableListOf()
    private val isChangedPredicates : MutableList<()->Boolean> = mutableListOf()
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