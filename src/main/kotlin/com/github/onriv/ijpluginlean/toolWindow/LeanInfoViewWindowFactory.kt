package com.github.onriv.ijpluginlean.toolWindow

// https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html#ui-dsl-basics
import com.github.onriv.ijpluginlean.services.ExternalInfoViewService
import com.github.onriv.ijpluginlean.services.MyProjectService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.*
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import javax.swing.*


// TODO not shown if indexing, the doc seems saying it's an option for it
class LeanInfoViewWindowFactory : ToolWindowFactory {

    companion object {
        fun updateGoal(project: Project, file: VirtualFile, caret: Caret, plainGoal: List<String>, plainTermGoal: String) {
            // from https://stackoverflow.com/questions/66548934/how-to-access-components-inside-a-custom-toolwindow-from-an-actios
            val infoViewWindow = ToolWindowManager.getInstance(project).getToolWindow("LeanInfoViewWindow")!!.contentManager.contents[0].component as
                    LeanInfoViewWindowFactory.LeanInfoViewWindow

            val contentBuilder = StringBuilder("▼ ${file.name}:${caret.logicalPosition.line+1}:${caret.logicalPosition.column}\n")
            if (plainGoal.isEmpty() && plainTermGoal.isEmpty()) {
                contentBuilder.append("No info found.\n")
            } else {
                if (plainGoal.isNotEmpty()) {
                    contentBuilder.append(" ▼ Tactic state\n")
                    if (plainGoal.size == 1) {
                        contentBuilder.append(" 1 goal\n")
                    } else {
                        contentBuilder.append(" ${plainGoal.size} goals\n")
                    }
                    for (s in plainGoal) {
                        contentBuilder.append(s)
                        contentBuilder.append("\n")
                    }
                }
                if (plainTermGoal.isNotEmpty()) {
                    contentBuilder.append(" ▼ Expected type\n")
                    contentBuilder.append(plainTermGoal)
                }
            }



            infoViewWindow.updateGoal(contentBuilder.toString())
        }


    }

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val leanInfoViewWindow = LeanInfoViewWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(leanInfoViewWindow, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class LeanInfoViewWindow(val toolWindow: ToolWindow) : SimpleToolWindowPanel(true) {

        private val service = toolWindow.project.service<MyProjectService>()
        private val infoViewService = toolWindow.project.service<ExternalInfoViewService>()
        private val goals = JEditorPane()
        private var editor : EditorEx

        private val BORDER = BorderFactory.createEmptyBorder(3, 0, 5, 0)

        init {
            // TODO this is copy from intellij-arend and it's wrong (it's fro SearchRender in intellij-arend)
            goals.contentType = "text/html"
            goals.border = BORDER
            goals.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            goals.font = JBTextArea().font

            editor = EditorFactory.getInstance().createViewer(DocumentImpl(" ", true), toolWindow.project) as EditorEx
            with(editor.settings) {
                isRightMarginShown = false
                isLineNumbersShown = false
                isLineMarkerAreaShown = false
                isRefrainFromScrolling = true
                isCaretRowShown = false
                isUseSoftWraps = true
                setGutterIconsShown(false)
                additionalLinesCount = 0
                additionalColumnsCount = 1
                isFoldingOutlineShown = false
                isVirtualSpace = false
            }
            editor.headerComponent = null
            editor.setCaretEnabled(false)
            editor.setHorizontalScrollbarVisible(false)
            editor.setVerticalScrollbarVisible(false)
            editor.isRendererMode = true

            val list = JBList(arrayOf("Item 1", "Item 2", "Item 3"))
            list.selectionMode = ListSelectionModel.SINGLE_SELECTION

            val scrollPane = JBScrollPane()
            scrollPane.isVisible = true // Initially visible

            // Add your custom logic to toggle visibility (e.g., button click)

            scrollPane.add(list)
            // scrollPane.add(editor.component)

            // setContent(editor.component)

//            val mySplitter = JBSplitter(true, 0.35f, 0.15f, 0.85f)
//            mySplitter.setDividerWidth(3);
//            val myComponent = JBUI.Panels.simplePanel();
//            // myComponent.add(mySplitter, BorderLayout.CENTER);
//            val currentMessages = JBUI.Panels.simplePanel()
//            val tacticState = JBUI.Panels.simplePanel()
//            val goals = JBUI.Panels.simplePanel()
//            val expectedType = JBUI.Panels.simplePanel()
            val expectedTypeEditor = createEditor()
            expectedTypeEditor.document.setText("""⊢ ∀ {C : Type u} [inst : Category.{v, u} C] [inst_1 : HasLimits C] {D : Type u'} [inst_2 : Category.{v', u'} D] [inst_3 : HasLimits D] (F : C ⥤ D) [inst_4 : PreservesLimits F] {W X Y Z S T : C} (f₁ : W ⟶ S) (f₂ : X ⟶ S) (g₁ : Y ⟶ T) (g₂ : Z ⟶ T) (i₁ : W ⟶ Y) (i₂ : X ⟶ Z) (i₃ : S ⟶ T) (eq₁ : f₁ ≫ i₃ = i₁ ≫ g₁) (eq₂ : f₂ ≫ i₃ = i₂ ≫ g₂), F.map (pullback.map f₁ f₂ g₁ g₂ i₁ i₂ i₃ eq₁ eq₂) ≫ (PreservesPullback.iso F g₁ g₂).hom = (PreservesPullback.iso F f₁ f₂).hom ≫ pullback.map (F.map f₁) (F.map f₂) (F.map g₁) (F.map g₂) (F.map i₁) (F.map i₂) (F.map i₃) ⋯ ⋯ """)
//            expectedType.add(JBLabel("▼ Current Message"))
//            expectedType.add(expectedTypeEditor.component)
//            tacticState.add(goals)
//            currentMessages.add(tacticState)
//            currentMessages.add(expectedType)
////                JBLabel("▼ Current ")
//            myComponent.add(currentMessages)
//            // mySplitter.firstComponent = loadingLabel
//            val secondComponent = JBUI.Panels.simplePanel(editor.component)
            // mySplitter.secondComponent = secondComponent

//            val myComponent = object : DialogPanel {
//                collapsibleGroup("Title") {
//                    row("Row:") {
//                        textField()
//                    }
//                }
//            }
//            val component = panel {
//                panel {
//                    collapsibleGroup("Current Line (TODO)") {
//                        collapsibleGroup("Tactic State (TODO)") {
//                            row {
//                                cell(expectedTypeEditor.component)
//                            }
//                        }
//                    }
//                }
//                panel {
//                    collapsibleGroup("All Messages") {
//                    }
//                }
//
////                rowsRange {
////                    row("Panel.rowsRange row:") {
////                        textField()
////                    }.rowComment("Panel.rowsRange is similar to Panel.panel but uses the same grid as parent. " +
////                            "Useful when grouped content should be managed together with RowsRange.enabledIf for example")
////                }
////
////                group("Panel.group") {
////                    row("Panel.group row:") {
////                        textField()
////                    }.rowComment("Panel.group adds panel with independent grid, title and some vertical space before/after the group")
////                }
////
////                groupRowsRange("Panel.groupRowsRange") {
////                    row("Panel.groupRowsRange row:") {
////                        textField()
////                    }.rowComment("Panel.groupRowsRange is similar to Panel.group but uses the same grid as parent. " +
////                            "See how aligned Panel.rowsRange row")
////                }
////
////                collapsibleGroup("Panel.collapsible&Group") {
////                    row("Panel.collapsibleGroup row:") {
////                        textField()
////                    }.rowComment("Panel.collapsibleGroup adds collapsible panel with independent grid, title and some vertical " +
////                            "space before/after the group. The group title is focusable via the Tab key and supports mnemonics")
////                }
////
////                var value = true
////                buttonsGroup("Panel.buttonGroup:") {
////                    row {
////                        radioButton("true", true)
////                    }
////                    row {
////                        radioButton("false", false)
////                    }.rowComment("Panel.buttonGroup unions Row.radioButton in one group. Must be also used for Row.checkBox if they are grouped " +
////                            "with some title")
////                }.bind({ value }, { value = it })
////
////                separator()
////                    .rowComment("Use separator() for horizontal separator")
////
////                row {
////                    label("Use Row.panel for creating panel in a cell:")
////                    panel {
////                        row("Sub panel row 1:") {
////                            textField()
////                        }
////                        row("Sub panel row 2:") {
////                            textField()
////                        }
////                    }
////                }
//            }



            // setContent(component)
            // setContent(createInfoView())

            setContent(editor.component)

            // TODO this is commented out, it's no easy to impl
            //      interactive goal inside intellij idea
            // editor.addEditorMouseMotionListener(InfoViewHoverListener())
            // TODO it must setup a language for the goal infoview...
            // editor = EditorFactory.
        }

        fun updateGoal(goal: String) {
            // for thread model and update ui:
            // - https://intellij-support.jetbrains.com/hc/en-us/community/posts/360009458040-Error-writing-data-in-a-tree-provided-by-a-background-thread
            // - https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html
            ApplicationManager.getApplication().invokeLater {
                editor.document.setText(goal)
                goals.text = goal
            }
//            goals.revalidate();
//            goals.updateUI();
        }

        fun createEditor(): EditorEx {
            val editor = EditorFactory.getInstance().createViewer(DocumentImpl(" ", true), toolWindow.project) as EditorEx
            with(editor.settings) {
                isRightMarginShown = false
                isLineNumbersShown = false
                isLineMarkerAreaShown = false
                isRefrainFromScrolling = true
                isCaretRowShown = false
                isUseSoftWraps = true
                setGutterIconsShown(false)
                additionalLinesCount = 0
                additionalColumnsCount = 1
                isFoldingOutlineShown = false
                isVirtualSpace = false
            }
            editor.headerComponent = null
            editor.setCaretEnabled(false)
            editor.setHorizontalScrollbarVisible(false)
            editor.setVerticalScrollbarVisible(false)
            editor.isRendererMode = true
            return editor
        }
        fun demoBasics(): DialogPanel {
//            return panel {
//                row("Row1 label:") {
//                    textField()
//                    label("Some text")
//                }
//
//                row("Row2:") {
//                    label("This text is aligned with previous row")
//                }
//
//                row("Row3:") {
//                    label("Rows 3 and 4 are in common parent grid")
//                    textField()
//                }.layout(RowLayout.PARENT_GRID)
//
//                row("Row4:") {
//                    textField()
//                    label("Rows 3 and 4 are in common parent grid")
//                }.layout(RowLayout.PARENT_GRID)
//            }
            return panel {
                panel {
                    collapsibleGroup("Current Line (TODO)") {
                        collapsibleGroup("Tactic State (TODO)") {
                            row {
                                cell(createEditor().component)
                            }
                        }
                    }
                }
                panel {
                    collapsibleGroup("▼ All Messages") {
                    }
                }

//                rowsRange {
//                    row("Panel.rowsRange row:") {
//                        textField()
//                    }.rowComment("Panel.rowsRange is similar to Panel.panel but uses the same grid as parent. " +
//                            "Useful when grouped content should be managed together with RowsRange.enabledIf for example")
//                }
//
//                group("Panel.group") {
//                    row("Panel.group row:") {
//                        textField()
//                    }.rowComment("Panel.group adds panel with independent grid, title and some vertical space before/after the group")
//                }
//
//                groupRowsRange("Panel.groupRowsRange") {
//                    row("Panel.groupRowsRange row:") {
//                        textField()
//                    }.rowComment("Panel.groupRowsRange is similar to Panel.group but uses the same grid as parent. " +
//                            "See how aligned Panel.rowsRange row")
//                }
//
//                collapsibleGroup("Panel.collapsible&Group") {
//                    row("Panel.collapsibleGroup row:") {
//                        textField()
//                    }.rowComment("Panel.collapsibleGroup adds collapsible panel with independent grid, title and some vertical " +
//                            "space before/after the group. The group title is focusable via the Tab key and supports mnemonics")
//                }
//
//                var value = true
//                buttonsGroup("Panel.buttonGroup:") {
//                    row {
//                        radioButton("true", true)
//                    }
//                    row {
//                        radioButton("false", false)
//                    }.rowComment("Panel.buttonGroup unions Row.radioButton in one group. Must be also used for Row.checkBox if they are grouped " +
//                            "with some title")
//                }.bind({ value }, { value = it })
//
//                separator()
//                    .rowComment("Use separator() for horizontal separator")
//
//                row {
//                    label("Use Row.panel for creating panel in a cell:")
//                    panel {
//                        row("Sub panel row 1:") {
//                            textField()
//                        }
//                        row("Sub panel row 2:") {
//                            textField()
//                        }
//                    }
//                }
            }
        }

        fun createInfoView(): JComponent {
            // Create your custom SimpleToolWindowPanel
            // val panel = SimpleToolWindowPanel(true)
            val panel = ScrollPaneFactory.createScrollPane()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

            // Add components (including TitledSeparators) to the panel
            // panel.setLayout(BoxLayout(panel, BoxLayout.Y_AXIS))

            // First TitledSeparator
            val separator1 = TitledSeparator("Section 1")
            panel.add(separator1)
            // Add other components related to Section 1 here...

            // Second TitledSeparator
            val separator2 = TitledSeparator("Section 2")
            panel.add(separator2)
            // Add other components related to Section 2 here...
            return panel
        }

    }
}
