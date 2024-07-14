package com.github.onriv.ijpluginlean.toolWindow

// https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html#ui-dsl-basics
import com.github.onriv.ijpluginlean.lsp.data.CodeWithInfosTag
import com.github.onriv.ijpluginlean.lsp.data.InteractiveGoals
import com.github.onriv.ijpluginlean.lsp.data.gson
import com.github.onriv.ijpluginlean.services.ExternalInfoViewService
import com.github.onriv.ijpluginlean.services.MyProjectService
import com.intellij.codeInsight.documentation.DocumentationHtmlUtil.docPopupPreferredMaxWidth
import com.intellij.codeInsight.documentation.DocumentationHtmlUtil.docPopupPreferredMinWidth
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.filters.ShowTextPopupHyperlinkInfo
import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.EditorSettingsProvider
import com.intellij.ui.EditorTextField
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.scale.JBUIScale.scale
import com.intellij.ui.util.maximumWidth
import com.intellij.ui.util.preferredHeight
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.awt.Color
import java.awt.Dimension
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

        fun updateInteractiveGoal(project: Project, file: VirtualFile?, caret: Caret, interactiveGoalsAny: Any) {
            // from https://stackoverflow.com/questions/66548934/how-to-access-components-inside-a-custom-toolwindow-from-an-actios
            val infoViewWindow = ToolWindowManager.getInstance(project).getToolWindow("LeanInfoViewWindow")!!.contentManager.contents[0].component as
                    LeanInfoViewWindowFactory.LeanInfoViewWindow
            val interactiveGoals : InteractiveGoals = gson.fromJson(gson.toJson(interactiveGoalsAny), InteractiveGoals::class.java)
            val interactiveGoalsText = interactiveGoals.toInfoViewString()

            ApplicationManager.getApplication().invokeLater {
                val infoViewWindowEditorEx: EditorEx = infoViewWindow.createEditor()
                infoViewWindowEditorEx.document.setText(interactiveGoalsText)
                val support = EditorHyperlinkSupport.get(infoViewWindowEditorEx)
                infoViewWindow.setContent(infoViewWindowEditorEx.component)
                infoViewWindowEditorEx.addEditorMouseMotionListener(object : EditorMouseMotionListener {
                    var hyperLink: RangeHighlighter? = null
                    override fun mouseMoved(e: EditorMouseEvent) {
                        if (hyperLink != null) {
                            support.removeHyperlink(hyperLink!!)
                        }
                        if (!e.isOverText) {
                            return
                        }
                        val c = interactiveGoals.getCodeText(e.offset) ?: return
                        var codeWithInfosTag : CodeWithInfosTag? = null
                        if (c is CodeWithInfosTag) {
                            codeWithInfosTag = c
                        } else if (c.parent != null && c.parent!! is CodeWithInfosTag) {
                            codeWithInfosTag = c.parent!! as CodeWithInfosTag
                        } else if (c.parent != null && c.parent!!.parent != null && c.parent!!.parent!! is CodeWithInfosTag) {
                            codeWithInfosTag = c.parent!!.parent!! as CodeWithInfosTag
                        }
                        if (codeWithInfosTag == null) {
                            return
                        }

                        if (c.parent == null || c.parent!!.parent == null) {
                            return
                        }
                        hyperLink = support.createHyperlink(
                            c.parent!!.startOffset,
                            c.parent!!.endOffset,
                            object : TextAttributes() {
                                override fun getBackgroundColor(): Color {
                                    return Color.decode("#add6ff")
                                }
                            },
                            CodeWithInfosDocumentationHyperLink(infoViewWindow, file!!, caret, codeWithInfosTag)
                        )
                    }
                })
            }
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
        private var editor : EditorEx = createEditor()

        private val BORDER = BorderFactory.createEmptyBorder(3, 0, 5, 0)

//         init {
//             // TODO this is copy from intellij-arend and it's wrong (it's fro SearchRender in intellij-arend)
//             goals.contentType = "text/html"
//             goals.border = BORDER
//             goals.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
//             goals.font = JBTextArea().font
//
//             editor = EditorFactory.getInstance().createViewer(DocumentImpl(" ", true), toolWindow.project) as EditorEx
//             with(editor.settings) {
//                 isRightMarginShown = false
//                 isLineNumbersShown = false
//                 isLineMarkerAreaShown = false
//                 isRefrainFromScrolling = true
//                 isCaretRowShown = false
//                 isUseSoftWraps = true
//                 setGutterIconsShown(false)
//                 additionalLinesCount = 0
//                 additionalColumnsCount = 1
//                 isFoldingOutlineShown = false
//                 isVirtualSpace = false
//             }
//             editor.headerComponent = null
//             editor.setCaretEnabled(false)
//             editor.setHorizontalScrollbarVisible(false)
//             editor.setVerticalScrollbarVisible(false)
//             editor.isRendererMode = true
//
//             val list = JBList(arrayOf("Item 1", "Item 2", "Item 3"))
//             list.selectionMode = ListSelectionModel.SINGLE_SELECTION
//
//             val scrollPane = JBScrollPane()
//             scrollPane.isVisible = true // Initially visible
//
//             // Add your custom logic to toggle visibility (e.g., button click)
//
//             scrollPane.add(list)
//             // scrollPane.add(editor.component)
//
//             // setContent(editor.component)
//
// //            val mySplitter = JBSplitter(true, 0.35f, 0.15f, 0.85f)
// //            mySplitter.setDividerWidth(3);
// //            val myComponent = JBUI.Panels.simplePanel();
// //            // myComponent.add(mySplitter, BorderLayout.CENTER);
// //            val currentMessages = JBUI.Panels.simplePanel()
// //            val tacticState = JBUI.Panels.simplePanel()
// //            val goals = JBUI.Panels.simplePanel()
// //            val expectedType = JBUI.Panels.simplePanel()
// //             val expectedTypeEditor = createEditor()
// //             expectedTypeEditor.document.setText("""⊢ ∀ {C : Type u} [inst : Category.{v, u} C] [inst_1 : HasLimits C] {D : Type u'} [inst_2 : Category.{v', u'} D] [inst_3 : HasLimits D] (F : C ⥤ D) [inst_4 : PreservesLimits F] {W X Y Z S T : C} (f₁ : W ⟶ S) (f₂ : X ⟶ S) (g₁ : Y ⟶ T) (g₂ : Z ⟶ T) (i₁ : W ⟶ Y) (i₂ : X ⟶ Z) (i₃ : S ⟶ T) (eq₁ : f₁ ≫ i₃ = i₁ ≫ g₁) (eq₂ : f₂ ≫ i₃ = i₂ ≫ g₂), F.map (pullback.map f₁ f₂ g₁ g₂ i₁ i₂ i₃ eq₁ eq₂) ≫ (PreservesPullback.iso F g₁ g₂).hom = (PreservesPullback.iso F f₁ f₂).hom ≫ pullback.map (F.map f₁) (F.map f₂) (F.map g₁) (F.map g₂) (F.map i₁) (F.map i₂) (F.map i₃) ⋯ ⋯ """)
// //            expectedType.add(JBLabel("▼ Current Message"))
// //            expectedType.add(expectedTypeEditor.component)
// //            tacticState.add(goals)
// //            currentMessages.add(tacticState)
// //            currentMessages.add(expectedType)
// ////                JBLabel("▼ Current ")
// //            myComponent.add(currentMessages)
// //            // mySplitter.firstComponent = loadingLabel
// //            val secondComponent = JBUI.Panels.simplePanel(editor.component)
//             // mySplitter.secondComponent = secondComponent
//
// //            val myComponent = object : DialogPanel {
// //                collapsibleGroup("Title") {
// //                    row("Row:") {
// //                        textField()
// //                    }
// //                }
// //            }
// //            val component = panel {
// //                panel {
// //                    collapsibleGroup("Current Line (TODO)") {
// //                        collapsibleGroup("Tactic State (TODO)") {
// //                            row {
// //                                cell(expectedTypeEditor.component)
// //                            }
// //                        }
// //                    }
// //                }
// //                panel {
// //                    collapsibleGroup("All Messages") {
// //                    }
// //                }
// //
// ////                rowsRange {
// ////                    row("Panel.rowsRange row:") {
// ////                        textField()
// ////                    }.rowComment("Panel.rowsRange is similar to Panel.panel but uses the same grid as parent. " +
// ////                            "Useful when grouped content should be managed together with RowsRange.enabledIf for example")
// ////                }
// ////
// ////                group("Panel.group") {
// ////                    row("Panel.group row:") {
// ////                        textField()
// ////                    }.rowComment("Panel.group adds panel with independent grid, title and some vertical space before/after the group")
// ////                }
// ////
// ////                groupRowsRange("Panel.groupRowsRange") {
// ////                    row("Panel.groupRowsRange row:") {
// ////                        textField()
// ////                    }.rowComment("Panel.groupRowsRange is similar to Panel.group but uses the same grid as parent. " +
// ////                            "See how aligned Panel.rowsRange row")
// ////                }
// ////
// ////                collapsibleGroup("Panel.collapsible&Group") {
// ////                    row("Panel.collapsibleGroup row:") {
// ////                        textField()
// ////                    }.rowComment("Panel.collapsibleGroup adds collapsible panel with independent grid, title and some vertical " +
// ////                            "space before/after the group. The group title is focusable via the Tab key and supports mnemonics")
// ////                }
// ////
// ////                var value = true
// ////                buttonsGroup("Panel.buttonGroup:") {
// ////                    row {
// ////                        radioButton("true", true)
// ////                    }
// ////                    row {
// ////                        radioButton("false", false)
// ////                    }.rowComment("Panel.buttonGroup unions Row.radioButton in one group. Must be also used for Row.checkBox if they are grouped " +
// ////                            "with some title")
// ////                }.bind({ value }, { value = it })
// ////
// ////                separator()
// ////                    .rowComment("Use separator() for horizontal separator")
// ////
// ////                row {
// ////                    label("Use Row.panel for creating panel in a cell:")
// ////                    panel {
// ////                        row("Sub panel row 1:") {
// ////                            textField()
// ////                        }
// ////                        row("Sub panel row 2:") {
// ////                            textField()
// ////                        }
// ////                    }
// ////                }
// //            }
//
//
//
//             // setContent(component)
//             // setContent(createInfoView())
//
//             // TODO this is for testing, remove it
//             val s = tryInteractive()
//             // val t = render(s as Map<*, *>)
//             val goalText = s.toInfoViewString()
//             editor.document.setText(goalText)
//             val support = EditorHyperlinkSupport.get(editor)
// //            support.addHighlighter(20, 30,
// //                 TextAttributesKey.createTextAttributesKey("PROPERTIES.KEY", DefaultLanguageHighlighterColors.KEYWORD).defaultAttributes
// //                )
// //            support.createHyperlink(1, 10,
// //                null,
// //                // TextAttributesKey.createTextAttributesKey("PROPERTIES.KEY", DefaultLanguageHighlighterColors.KEYWORD).defaultAttributes,
// //                ShowTextPopupHyperlinkInfo("TODO", "docod"))
// //            var createHyperlink = support.createHyperlink(
// //                0, 1,
// //                null,
// //                // TextAttributesKey.createTextAttributesKey("PROPERTIES.KEY", DefaultLanguageHighlighterColors.KEYWORD).defaultAttributes,
// //                ShowTextPopupHyperlinkInfo("TODO11", "docod111")
// //            )
// //            var createHyperlink1 = support.createHyperlink(
// //                10, 12,
// //                null,
// //                // TextAttributesKey.createTextAttributesKey("PROPERTIES.KEY", DefaultLanguageHighlighterColors.KEYWORD).defaultAttributes,
// //                ShowTextPopupHyperlinkInfo("TODO11", "docod111")
// //            )
// //             setContent(editor.component)
// //            editor.addEditor
//             editor.addEditorMouseMotionListener(object : EditorMouseMotionListener {
//                 var hyperLink: RangeHighlighter? = null
//                 override fun mouseMoved(e: EditorMouseEvent) {
//                     if (hyperLink != null) {
//                         support.removeHyperlink(hyperLink!!)
//                     }
//                     if (!e.isOverText) {
//                         return
//                     }
//                     var c = s.getCodeText(e.offset)
//                     if (c == null) {
//                         return
//                     }
//                     val src = """
//                                 Structure instance. `{ x := e, ... }` assigns `e` to field `x`, which may be
//                                 inherited. If `e` is itself a variable called `x`, it can be elided:
//                                 `fun y => { x := 1, y }`.
//                                 A *structure update* of an existing value can be given via `with`:
//                                 `{ point with x := 1 }`.The structure type can be specified if not inferable:
//                                 `{ x := 1, y := 2 : Point }`.
//                                 """.trimIndent()
//                     val flavour = CommonMarkFlavourDescriptor()
//                     val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(src)
//                     val html = HtmlGenerator(src, parsedTree, flavour).generateHtml()
//                     hyperLink = support.createHyperlink(
//                         c.startOffset,
//                         c.endOffset,
//                         object : TextAttributes() {
//                             override fun getBackgroundColor(): Color {
//                                 return Color.decode("#add6ff")
//                             }
//                         },
//                         // ref ShowTextPopupHyperlinkInfo
//                         object : HyperlinkInfo {
//                             override fun navigate(project: Project) {
//                                 ApplicationManager.getApplication().invokeLater {
// //                                    val document = EditorFactory.getInstance().createDocument(StringUtil.convertLineSeparators(text))
// //                                    val textField = object : EditorTextField(document, project, FileTypes.PLAIN_TEXT, true, false) {
// //                                        override fun createEditor(): EditorEx {
// //                                            val editor = super.createEditor()
// //                                            editor.scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
// //                                            editor.scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
// //                                            editor.settings.isUseSoftWraps = true
// //                                            return editor
// //                                        }
// //                                    }
// //
//                                     // ref : https://youtrack.jetbrains.com/issue/IJPL-148864/Cant-change-link-style-when-using-JBHtmlEditorKit-JBHtmlPane
//                                     // TODO handling style for this
//                                     val doc = JEditorPane().apply {
//                                         contentType = "text/html"
//                                         text = html
//                                     }
//                                     val expr = createEditor()
//                                     val testT = """(match a, b with
//     | { x := x₁, y := y₁, z := z₁ }, { x := x₂, y := y₂, z := z₂ } => { x := x₁ + x₂, y := y₁ + y₂, z := z₁ + z₂ }).x =
//   (match b, a with
//     | { x := x₁, y := y₁, z := z₁ }, { x := x₂, y := y₂, z := z₂ } => { x := x₁ + x₂, y := y₁ + y₂, z := z₁ + z₂ }).x"""
//                                     expr.document.setText("""(match a, b with
//     | { x := x₁, y := y₁, z := z₁ }, { x := x₂, y := y₂, z := z₂ } => { x := x₁ + x₂, y := y₁ + y₂, z := z₁ + z₂ }).x =
//   (match b, a with
//     | { x := x₁, y := y₁, z := z₁ }, { x := x₂, y := y₂, z := z₂ } => { x := x₁ + x₂, y := y₁ + y₂, z := z₁ + z₂ }).x""")
//                                     val document = EditorFactory.getInstance().createDocument(StringUtil.convertLineSeparators(testT))
//                                     val textField = object : EditorTextField(document, project, FileTypes.PLAIN_TEXT, true, false) {
//                                         override fun createEditor(): EditorEx {
//                                             val editor = super.createEditor()
//                                             editor.scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
//                                             editor.scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
//                                             editor.settings.isUseSoftWraps = true
//                                             return editor
//                                         }
//                                     }
//                                     val popup1 = JBScrollPane()
//                                     popup1.add(expr.component)
//                                     popup1.add(doc)
//                                     val popup = panel {
//                                         row {
//                                             scrollCell(expr.component)
//                                         }
//                                         row {
//                                             scrollCell(doc)
//                                         }
//                                     }
//                                     val size = toolWindow.component.size
//                                     val fontSize = expr.colorsScheme.editorFontSize
//                                     val lineSpacing = expr.lineHeight
//                                     WindowManager.getInstance().getFrame(project)?.size?.let {
//                                          doc.size = Dimension(size.width*8/10, Short.MAX_VALUE.toInt())
//                                          val result = doc.preferredSize
//                                          doc.preferredSize = Dimension(size.width*8/10, result.height)
//                                          // TODO call size
//                                          expr.component.size = Dimension(size.width*8/10, Short.MAX_VALUE.toInt())
//                                          val result1 = expr.component.preferredSize
//                                         expr.component.size = Dimension(size.width*8/10, result1.height)
//                                     }
//                                     popup1.preferredSize = Dimension(size.width*8/10, size.height*2/5)
//                                     popup.withPreferredWidth(size.width*8/10)
//
//                                     // val p = object:JFrame() {
//                                     //     init {
//                                     //         add(expr.component)
//                                     //         add(doc)
//                                     //     }
//                                     //
//                                     //     // override fun getMinimumSize() : Dimension {
//                                     //     //     return Dimension(size.width*8/10, size.height)
//                                     //     // }
//                                     //     // override fun getPreferredSize(): Dimension {
//                                     //     //     // return super.getPreferredSize()
//                                     //     //     // expr.component.size = Dimension(size.width*8/10, Short.MAX_VALUE.toInt())
//                                     //     //     // doc.size = Dimension(size.width*8/10, Short.MAX_VALUE.toInt())
//                                     //     //     // expr.component.revalidate()
//                                     //     //     // doc.revalidate()
//                                     //     //     // return Dimension(size.width*8/10, 2*(expr.component.preferredHeight+doc.preferredHeight))
//                                     //     //     return Dimension(0, 300)
//                                     //     // }
//                                     // }
//                                     val p = JFrame()
//                                     p.layout = VerticalLayout(1)
//                                     // Add components vertically
//                                     p.add(expr.component)
//                                     p.add(doc)
//                                     p.pack()
//                                     p.size = Dimension(size.width*8/10, Short.MAX_VALUE.toInt())
//                                     p.revalidate()
//                                     val p1 = JPanel(VerticalLayout(1))
//                                     p1.add(expr.component)
//                                     p1.add(doc)
//                                     // p1.preferredSize = Dimension(size.width*8/10, expr.height)
//                                     val popup2 = object: JBScrollPane(p1) {
//                                         // override fun getPreferredSize(): Dimension {
//                                         //     // return super.getPreferredSize()
//                                         //     return p.size
//                                         // }
//
//                                         // override fun getMinimumSize() : Dimension {
//                                         //     return Dimension(size.width*8/10, size.height*6/10)
//                                         // }
//                                     }
// //                                    popup2.add(expr.component)
// //                                    popup2.add(doc)
// //                                    popup2.revalidate();
//                                    popup2.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
//                                    // popup2.preferredSize = p.size
//                                    // popup2.minimumHeight = doc.minimumHeight + expr.component.minimumHeight
//                                    //  popup2.size = Dimension(size.width*8/10, Math.min(doc.height + expr.component.height + 2, size.height*3/5))
//                                    popup2.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
//                                     // popup2.preferredSize = Dimension(size.width*8/10, size.height*2/5)
//                                     // popup2.withPreferredWidth(size.width*8/10)
//                                     // popup2.withPreferredHeight(size.height*8/10)
// //                                    val popup2 = DocumentationScrollPane(p)
//                                     val factory = JBPopupFactory.getInstance()
//                                     factory.createComponentPopupBuilder(popup2, popup2)
//                                         // .setTitle(title)
//                                         // .setResizable(true)
//                                         .setMovable(true)
//                                         .setRequestFocus(true)
//                                         .createPopup()
//                                         // .showInBestPositionFor(editor)
//                                         // .showInCenterOf(toolWindow.component)
//                                          // .showInFocusCenter()
//                                          .show(factory.guessBestPopupLocation(editor))
// //                                        .showCenteredInCurrentWindow(project)
//                                 }
//                             }
//
//                         }
//                     )
//                 }
//             })
//             editor.addEditorMouseListener(object : EditorMouseListener {
//
//             })
//             var editorTextField = EditorTextField("""S01_Structures.lean:80:9
// Tactic state
// 3 goals
// case x
// a b : Point
// ⊢ (match a, b with
//     | { x := x₁, y := y₁, z := z₁ }, { x := x₂, y := y₂, z := z₂ } => { x := x₁ + x₂, y := y₁ + y₂, z := z₁ + z₂ }).x =
//   (match b, a with
//     | { x := x₁, y := y₁, z := z₁ }, { x := x₂, y := y₂, z := z₂ } => { x := x₁ + x₂, y := y₁ + y₂, z := z₁ + z₂ }).x
// case y
// a b : Point
// ⊢ (match a, b with
//     | { x := x₁, y := y₁, z := z₁ }, { x := x₂, y := y₂, z := z₂ } => { x := x₁ + x₂, y := y₁ + y₂, z := z₁ + z₂ }).y =
//   (match b, a with
//     | { x := x₁, y := y₁, z := z₁ }, { x := x₂, y := y₂, z := z₂ } => { x := x₁ + x₂, y := y₁ + y₂, z := z₁ + z₂ }).y
// case z
// a b : Point
// ⊢ (match a, b with
//     | { x := x₁, y := y₁, z := z₁ }, { x := x₂, y := y₂, z := z₂ } => { x := x₁ + x₂, y := y₁ + y₂, z := z₁ + z₂ }).z =
//   (match b, a with
//     | { x := x₁, y := y₁, z := z₁ }, { x := x₂, y := y₂, z := z₂ } => { x := x₁ + x₂, y := y₁ + y₂, z := z₁ + z₂ }).z
//             """, toolWindow.project, PlainTextFileType.INSTANCE)
//             editorTextField.addSettingsProvider(object : EditorSettingsProvider {
//                 override fun customizeSettings(editor: EditorEx?) {
//                     if (editor == null) {
//                         return
//                     }
//                     with(editor.settings) {
//                         isRightMarginShown = false
//                         isLineNumbersShown = false
//                         isLineMarkerAreaShown = false
//                         isRefrainFromScrolling = true
//                         isCaretRowShown = false
//                         isUseSoftWraps = true
//                         setGutterIconsShown(false)
//                         additionalLinesCount = 0
//                         additionalColumnsCount = 1
//                         isFoldingOutlineShown = false
//                         isVirtualSpace = false
//                     }
//                     editor.headerComponent = null
//                     editor.setCaretEnabled(false)
//                     editor.setHorizontalScrollbarVisible(false)
//                     editor.setVerticalScrollbarVisible(true)
//                     editor.isRendererMode = false
//                 }
//             })
//             val editorColorsManager = EditorColorsManager.getInstance()
//             val editorFont  = editorColorsManager.globalScheme.getFont(EditorFontType.PLAIN)
//               editorTextField.font = editorFont
//              // setContent(editorTextField)
//
//
//
// //            editor.addFocusListener(object: FocusChangeListener(){
// //
// //            })
//
//
//             // TODO this is commented out, it's no easy to impl
//             //      interactive goal inside intellij idea
//             // editor.addEditorMouseMotionListener(InfoViewHoverListener())
//             // TODO it must setup a language for the goal infoview...
//             // editor = EditorFactory.
//         }

        // TODO
        private fun render(map: Map<*, *>): String {
            for (g in map["goals"] as List<*>) {
                return ""
            }
            return ""
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
            // val editor = editorTextField.getEditor(true)!!
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

        fun tryInteractive() : InteractiveGoals {
            return gson.fromJson("""
                {
                    "goals": [
                        {
                            "userName": "x",
                            "type": {
                                "tag": [
                                    {
                                        "subexprPos": "/",
                                        "info": {
                                            "p": "1"
                                        }
                                    },
                                    {
                                        "append": [
                                            {
                                                "tag": [
                                                    {
                                                        "subexprPos": "/0/1",
                                                        "info": {
                                                            "p": "2"
                                                        },
                                                        "diffStatus": "willChange"
                                                    },
                                                    {
                                                        "append": [
                                                            {
                                                                "tag": [
                                                                    {
                                                                        "subexprPos": "/0/1/1",
                                                                        "info": {
                                                                            "p": "3"
                                                                        }
                                                                    },
                                                                    {
                                                                        "append": [
                                                                            {
                                                                                "text": "("
                                                                            },
                                                                            {
                                                                                "tag": [
                                                                                    {
                                                                                        "subexprPos": "/0/1/1",
                                                                                        "info": {
                                                                                            "p": "4"
                                                                                        }
                                                                                    },
                                                                                    {
                                                                                        "append": [
                                                                                            {
                                                                                                "text": "match "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/0/0/1",
                                                                                                        "info": {
                                                                                                            "p": "5"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "text": "a"
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": ", "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/0/1",
                                                                                                        "info": {
                                                                                                            "p": "6"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "text": "b"
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": " with\n    | "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1",
                                                                                                        "info": {
                                                                                                            "p": "7"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "8"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "9"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/2",
                                                                                                                        "info": {
                                                                                                                            "p": "10"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "11"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/3",
                                                                                                                        "info": {
                                                                                                                            "p": "12"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "13"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": ", "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1",
                                                                                                        "info": {
                                                                                                            "p": "14"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/0",
                                                                                                                        "info": {
                                                                                                                            "p": "15"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "16"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "17"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "18"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/2",
                                                                                                                        "info": {
                                                                                                                            "p": "19"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "20"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": " \u003d\u003e "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1",
                                                                                                        "info": {
                                                                                                            "p": "21"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/2",
                                                                                                                        "info": {
                                                                                                                            "p": "22"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "23"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/0/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "24"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "x₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/0/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "25"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "x₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/3",
                                                                                                                        "info": {
                                                                                                                            "p": "26"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "27"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "28"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "y₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "29"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "y₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/0",
                                                                                                                        "info": {
                                                                                                                            "p": "30"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "31"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "32"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "z₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "33"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "z₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            }
                                                                                        ]
                                                                                    }
                                                                                ]
                                                                            },
                                                                            {
                                                                                "text": ")"
                                                                            }
                                                                        ]
                                                                    }
                                                                ]
                                                            },
                                                            {
                                                                "text": ".x"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            },
                                            {
                                                "text": " \u003d\n  "
                                            },
                                            {
                                                "tag": [
                                                    {
                                                        "subexprPos": "/1",
                                                        "info": {
                                                            "p": "34"
                                                        },
                                                        "diffStatus": "willChange"
                                                    },
                                                    {
                                                        "append": [
                                                            {
                                                                "tag": [
                                                                    {
                                                                        "subexprPos": "/1/1",
                                                                        "info": {
                                                                            "p": "35"
                                                                        }
                                                                    },
                                                                    {
                                                                        "append": [
                                                                            {
                                                                                "text": "("
                                                                            },
                                                                            {
                                                                                "tag": [
                                                                                    {
                                                                                        "subexprPos": "/1/1",
                                                                                        "info": {
                                                                                            "p": "36"
                                                                                        }
                                                                                    },
                                                                                    {
                                                                                        "append": [
                                                                                            {
                                                                                                "text": "match "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/0/0/1",
                                                                                                        "info": {
                                                                                                            "p": "37"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "text": "b"
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": ", "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/0/1",
                                                                                                        "info": {
                                                                                                            "p": "38"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "text": "a"
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": " with\n    | "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1",
                                                                                                        "info": {
                                                                                                            "p": "39"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/2",
                                                                                                                        "info": {
                                                                                                                            "p": "40"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "41"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/3",
                                                                                                                        "info": {
                                                                                                                            "p": "42"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "43"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/0",
                                                                                                                        "info": {
                                                                                                                            "p": "44"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "45"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": ", "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1",
                                                                                                        "info": {
                                                                                                            "p": "46"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "47"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "48"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/2",
                                                                                                                        "info": {
                                                                                                                            "p": "49"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "50"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/3",
                                                                                                                        "info": {
                                                                                                                            "p": "51"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "52"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": " \u003d\u003e "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1",
                                                                                                        "info": {
                                                                                                            "p": "53"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/3/3",
                                                                                                                        "info": {
                                                                                                                            "p": "54"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "55"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/0/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "56"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "x₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/0/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "57"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "x₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/0",
                                                                                                                        "info": {
                                                                                                                            "p": "58"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "59"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "60"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "y₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "61"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "y₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "62"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "63"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "64"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "z₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "65"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "z₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            }
                                                                                        ]
                                                                                    }
                                                                                ]
                                                                            },
                                                                            {
                                                                                "text": ")"
                                                                            }
                                                                        ]
                                                                    }
                                                                ]
                                                            },
                                                            {
                                                                "text": ".x"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            },
                            "mvarId": "_uniq.3101",
                            "isInserted": false,
                            "hyps": [
                                {
                                    "type": {
                                        "tag": [
                                            {
                                                "subexprPos": "/",
                                                "info": {
                                                    "p": "0"
                                                }
                                            },
                                            {
                                                "text": "Point"
                                            }
                                        ]
                                    },
                                    "names": [
                                        "a",
                                        "b"
                                    ],
                                    "fvarIds": [
                                        "_uniq.3017",
                                        "_uniq.3018"
                                    ]
                                }
                            ],
                            "goalPrefix": "⊢ ",
                            "ctx": {
                                "p": "66"
                            }
                        },
                        {
                            "userName": "y",
                            "type": {
                                "tag": [
                                    {
                                        "subexprPos": "/",
                                        "info": {
                                            "p": "68"
                                        }
                                    },
                                    {
                                        "append": [
                                            {
                                                "tag": [
                                                    {
                                                        "subexprPos": "/0/1",
                                                        "info": {
                                                            "p": "69"
                                                        },
                                                        "diffStatus": "willChange"
                                                    },
                                                    {
                                                        "append": [
                                                            {
                                                                "tag": [
                                                                    {
                                                                        "subexprPos": "/0/1/1",
                                                                        "info": {
                                                                            "p": "70"
                                                                        }
                                                                    },
                                                                    {
                                                                        "append": [
                                                                            {
                                                                                "text": "("
                                                                            },
                                                                            {
                                                                                "tag": [
                                                                                    {
                                                                                        "subexprPos": "/0/1/1",
                                                                                        "info": {
                                                                                            "p": "71"
                                                                                        }
                                                                                    },
                                                                                    {
                                                                                        "append": [
                                                                                            {
                                                                                                "text": "match "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/0/0/1",
                                                                                                        "info": {
                                                                                                            "p": "72"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "text": "a"
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": ", "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/0/1",
                                                                                                        "info": {
                                                                                                            "p": "73"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "text": "b"
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": " with\n    | "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1",
                                                                                                        "info": {
                                                                                                            "p": "74"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "75"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "76"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/2",
                                                                                                                        "info": {
                                                                                                                            "p": "77"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "78"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/3",
                                                                                                                        "info": {
                                                                                                                            "p": "79"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "80"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": ", "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1",
                                                                                                        "info": {
                                                                                                            "p": "81"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/0",
                                                                                                                        "info": {
                                                                                                                            "p": "82"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "83"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "84"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "85"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/2",
                                                                                                                        "info": {
                                                                                                                            "p": "86"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "87"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": " \u003d\u003e "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1",
                                                                                                        "info": {
                                                                                                            "p": "88"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/2",
                                                                                                                        "info": {
                                                                                                                            "p": "89"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "90"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/0/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "91"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "x₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/0/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "92"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "x₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/3",
                                                                                                                        "info": {
                                                                                                                            "p": "93"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "94"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "95"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "y₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "96"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "y₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/0",
                                                                                                                        "info": {
                                                                                                                            "p": "97"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "98"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "99"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "z₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "100"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "z₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            }
                                                                                        ]
                                                                                    }
                                                                                ]
                                                                            },
                                                                            {
                                                                                "text": ")"
                                                                            }
                                                                        ]
                                                                    }
                                                                ]
                                                            },
                                                            {
                                                                "text": ".y"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            },
                                            {
                                                "text": " \u003d\n  "
                                            },
                                            {
                                                "tag": [
                                                    {
                                                        "subexprPos": "/1",
                                                        "info": {
                                                            "p": "101"
                                                        },
                                                        "diffStatus": "willChange"
                                                    },
                                                    {
                                                        "append": [
                                                            {
                                                                "tag": [
                                                                    {
                                                                        "subexprPos": "/1/1",
                                                                        "info": {
                                                                            "p": "102"
                                                                        }
                                                                    },
                                                                    {
                                                                        "append": [
                                                                            {
                                                                                "text": "("
                                                                            },
                                                                            {
                                                                                "tag": [
                                                                                    {
                                                                                        "subexprPos": "/1/1",
                                                                                        "info": {
                                                                                            "p": "103"
                                                                                        }
                                                                                    },
                                                                                    {
                                                                                        "append": [
                                                                                            {
                                                                                                "text": "match "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/0/0/1",
                                                                                                        "info": {
                                                                                                            "p": "104"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "text": "b"
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": ", "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/0/1",
                                                                                                        "info": {
                                                                                                            "p": "105"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "text": "a"
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": " with\n    | "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1",
                                                                                                        "info": {
                                                                                                            "p": "106"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/2",
                                                                                                                        "info": {
                                                                                                                            "p": "107"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "108"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/3",
                                                                                                                        "info": {
                                                                                                                            "p": "109"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "110"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/0",
                                                                                                                        "info": {
                                                                                                                            "p": "111"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "112"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": ", "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1",
                                                                                                        "info": {
                                                                                                            "p": "113"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "114"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "115"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/2",
                                                                                                                        "info": {
                                                                                                                            "p": "116"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "117"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/3",
                                                                                                                        "info": {
                                                                                                                            "p": "118"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "119"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": " \u003d\u003e "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1",
                                                                                                        "info": {
                                                                                                            "p": "120"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/3/3",
                                                                                                                        "info": {
                                                                                                                            "p": "121"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "122"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/0/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "123"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "x₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/0/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "124"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "x₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/0",
                                                                                                                        "info": {
                                                                                                                            "p": "125"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "126"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "127"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "y₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "128"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "y₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "129"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "130"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "131"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "z₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "132"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "z₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            }
                                                                                        ]
                                                                                    }
                                                                                ]
                                                                            },
                                                                            {
                                                                                "text": ")"
                                                                            }
                                                                        ]
                                                                    }
                                                                ]
                                                            },
                                                            {
                                                                "text": ".y"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            },
                            "mvarId": "_uniq.3102",
                            "isInserted": false,
                            "hyps": [
                                {
                                    "type": {
                                        "tag": [
                                            {
                                                "subexprPos": "/",
                                                "info": {
                                                    "p": "67"
                                                }
                                            },
                                            {
                                                "text": "Point"
                                            }
                                        ]
                                    },
                                    "names": [
                                        "a",
                                        "b"
                                    ],
                                    "fvarIds": [
                                        "_uniq.3017",
                                        "_uniq.3018"
                                    ]
                                }
                            ],
                            "goalPrefix": "⊢ ",
                            "ctx": {
                                "p": "133"
                            }
                        },
                        {
                            "userName": "z",
                            "type": {
                                "tag": [
                                    {
                                        "subexprPos": "/",
                                        "info": {
                                            "p": "135"
                                        }
                                    },
                                    {
                                        "append": [
                                            {
                                                "tag": [
                                                    {
                                                        "subexprPos": "/0/1",
                                                        "info": {
                                                            "p": "136"
                                                        },
                                                        "diffStatus": "willChange"
                                                    },
                                                    {
                                                        "append": [
                                                            {
                                                                "tag": [
                                                                    {
                                                                        "subexprPos": "/0/1/1",
                                                                        "info": {
                                                                            "p": "137"
                                                                        }
                                                                    },
                                                                    {
                                                                        "append": [
                                                                            {
                                                                                "text": "("
                                                                            },
                                                                            {
                                                                                "tag": [
                                                                                    {
                                                                                        "subexprPos": "/0/1/1",
                                                                                        "info": {
                                                                                            "p": "138"
                                                                                        }
                                                                                    },
                                                                                    {
                                                                                        "append": [
                                                                                            {
                                                                                                "text": "match "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/0/0/1",
                                                                                                        "info": {
                                                                                                            "p": "139"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "text": "a"
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": ", "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/0/1",
                                                                                                        "info": {
                                                                                                            "p": "140"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "text": "b"
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": " with\n    | "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1",
                                                                                                        "info": {
                                                                                                            "p": "141"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "142"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "143"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/2",
                                                                                                                        "info": {
                                                                                                                            "p": "144"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "145"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/3",
                                                                                                                        "info": {
                                                                                                                            "p": "146"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "147"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": ", "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1",
                                                                                                        "info": {
                                                                                                            "p": "148"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/0",
                                                                                                                        "info": {
                                                                                                                            "p": "149"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "150"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "151"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "152"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/2",
                                                                                                                        "info": {
                                                                                                                            "p": "153"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "154"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": " \u003d\u003e "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1",
                                                                                                        "info": {
                                                                                                            "p": "155"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/2",
                                                                                                                        "info": {
                                                                                                                            "p": "156"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "157"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/0/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "158"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "x₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/0/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "159"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "x₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/3",
                                                                                                                        "info": {
                                                                                                                            "p": "160"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "161"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "162"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "y₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/0/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "163"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "y₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/0",
                                                                                                                        "info": {
                                                                                                                            "p": "164"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "165"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "166"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "z₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/0/1/1/1/1/1/1/1/1/1/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "167"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "z₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            }
                                                                                        ]
                                                                                    }
                                                                                ]
                                                                            },
                                                                            {
                                                                                "text": ")"
                                                                            }
                                                                        ]
                                                                    }
                                                                ]
                                                            },
                                                            {
                                                                "text": ".z"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            },
                                            {
                                                "text": " \u003d\n  "
                                            },
                                            {
                                                "tag": [
                                                    {
                                                        "subexprPos": "/1",
                                                        "info": {
                                                            "p": "168"
                                                        },
                                                        "diffStatus": "willChange"
                                                    },
                                                    {
                                                        "append": [
                                                            {
                                                                "tag": [
                                                                    {
                                                                        "subexprPos": "/1/1",
                                                                        "info": {
                                                                            "p": "169"
                                                                        }
                                                                    },
                                                                    {
                                                                        "append": [
                                                                            {
                                                                                "text": "("
                                                                            },
                                                                            {
                                                                                "tag": [
                                                                                    {
                                                                                        "subexprPos": "/1/1",
                                                                                        "info": {
                                                                                            "p": "170"
                                                                                        }
                                                                                    },
                                                                                    {
                                                                                        "append": [
                                                                                            {
                                                                                                "text": "match "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/0/0/1",
                                                                                                        "info": {
                                                                                                            "p": "171"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "text": "b"
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": ", "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/0/1",
                                                                                                        "info": {
                                                                                                            "p": "172"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "text": "a"
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": " with\n    | "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1",
                                                                                                        "info": {
                                                                                                            "p": "173"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/2",
                                                                                                                        "info": {
                                                                                                                            "p": "174"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "175"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/3",
                                                                                                                        "info": {
                                                                                                                            "p": "176"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "177"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/0",
                                                                                                                        "info": {
                                                                                                                            "p": "178"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/0/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "179"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z₁"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": ", "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1",
                                                                                                        "info": {
                                                                                                            "p": "180"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "181"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "182"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/2",
                                                                                                                        "info": {
                                                                                                                            "p": "183"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "184"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/3",
                                                                                                                        "info": {
                                                                                                                            "p": "185"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/0/0/0/0/3/1/1/1/0/1/1/1/1/1/1/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "186"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z₂"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            },
                                                                                            {
                                                                                                "text": " \u003d\u003e "
                                                                                            },
                                                                                            {
                                                                                                "tag": [
                                                                                                    {
                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1",
                                                                                                        "info": {
                                                                                                            "p": "187"
                                                                                                        }
                                                                                                    },
                                                                                                    {
                                                                                                        "append": [
                                                                                                            {
                                                                                                                "text": "{ "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/3/3",
                                                                                                                        "info": {
                                                                                                                            "p": "188"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "x"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "189"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/0/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "190"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "x₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/0/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "191"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "x₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/0",
                                                                                                                        "info": {
                                                                                                                            "p": "192"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "y"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "193"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "194"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "y₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/0/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "195"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "y₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": ", "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/0/1",
                                                                                                                        "info": {
                                                                                                                            "p": "196"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "text": "z"
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " :\u003d "
                                                                                                            },
                                                                                                            {
                                                                                                                "tag": [
                                                                                                                    {
                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/1",
                                                                                                                        "info": {
                                                                                                                            "p": "197"
                                                                                                                        }
                                                                                                                    },
                                                                                                                    {
                                                                                                                        "append": [
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/1/0/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "198"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "z₁"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "text": " + "
                                                                                                                            },
                                                                                                                            {
                                                                                                                                "tag": [
                                                                                                                                    {
                                                                                                                                        "subexprPos": "/1/1/1/1/1/1/1/1/1/1/1",
                                                                                                                                        "info": {
                                                                                                                                            "p": "199"
                                                                                                                                        }
                                                                                                                                    },
                                                                                                                                    {
                                                                                                                                        "text": "z₂"
                                                                                                                                    }
                                                                                                                                ]
                                                                                                                            }
                                                                                                                        ]
                                                                                                                    }
                                                                                                                ]
                                                                                                            },
                                                                                                            {
                                                                                                                "text": " }"
                                                                                                            }
                                                                                                        ]
                                                                                                    }
                                                                                                ]
                                                                                            }
                                                                                        ]
                                                                                    }
                                                                                ]
                                                                            },
                                                                            {
                                                                                "text": ")"
                                                                            }
                                                                        ]
                                                                    }
                                                                ]
                                                            },
                                                            {
                                                                "text": ".z"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            },
                            "mvarId": "_uniq.3103",
                            "isInserted": false,
                            "hyps": [
                                {
                                    "type": {
                                        "tag": [
                                            {
                                                "subexprPos": "/",
                                                "info": {
                                                    "p": "134"
                                                }
                                            },
                                            {
                                                "text": "Point"
                                            }
                                        ]
                                    },
                                    "names": [
                                        "a",
                                        "b"
                                    ],
                                    "fvarIds": [
                                        "_uniq.3017",
                                        "_uniq.3018"
                                    ]
                                }
                            ],
                            "goalPrefix": "⊢ ",
                            "ctx": {
                                "p": "200"
                            }
                        }
                    ]
                }
                
            """, InteractiveGoals::class.java)
        }

    }

    /**
     * copy from com.intellij.codeInsight.documentation.DocumentationEditorPane#getPreferredContentWidth ...
     */
    private fun getPreferredContentWidth(textLength: Int): Int {
        // Heuristics to calculate popup width based on the amount of the content.
        // The proportions are set for 4 chars/1px in range between 200 and 1000 chars.
        // 200 chars and less is 300px, 1000 chars and more is 500px.
        // These values were calculated based on experiments with varied content and manual resizing to comfortable width.
        val contentLengthPreferredSize = if (textLength < 200) {
            docPopupPreferredMinWidth
        } else if (textLength > 200 && textLength < 1000) {
            docPopupPreferredMinWidth +
                    (textLength - 200) * (docPopupPreferredMaxWidth - docPopupPreferredMinWidth) / (1000 - 200)
        } else {
            docPopupPreferredMaxWidth
        }
        return scale(contentLengthPreferredSize)
    }

}
