package com.github.onriv.ijpluginlean.toolWindow

import com.github.onriv.ijpluginlean.lsp.LeanLspServerManager
import com.github.onriv.ijpluginlean.lsp.data.CodeWithInfos
import com.github.onriv.ijpluginlean.lsp.data.CodeWithInfosTag
import com.github.onriv.ijpluginlean.lsp.data.gson
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.awt.Dimension
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

class CodeWithInfosDocumentationHyperLink(
    val toolWindow: LeanInfoViewWindowFactory.LeanInfoViewWindow,
    val file: VirtualFile,
    val caret: Caret,
    val codeWithInfosTag: CodeWithInfosTag
) : HyperlinkInfo {
    override fun navigate(project: Project) {
        var infoToInteractive =
            LeanLspServerManager.getInstance(project)
                .infoToInteractive(file, caret, codeWithInfosTag.f0.info) as Map<String, Any>
        val type: CodeWithInfos = gson.fromJson(gson.toJson(infoToInteractive["type"]), CodeWithInfos::class.java)
        val exprExplicit: CodeWithInfos =
            gson.fromJson(gson.toJson(infoToInteractive["exprExplicit"]), CodeWithInfos::class.java)
        val markdownDoc: String = infoToInteractive["doc"] as String
        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownDoc)
        val htmlDoc = HtmlGenerator(markdownDoc, parsedTree, flavour).generateHtml()
        val typeStr = type.toInfoViewString(0, null)
        val exprStr = exprExplicit.toInfoViewString(0, null)
        createPopupPanel("$exprStr : $typeStr", htmlDoc)
    }

    fun createDocPanel(doc: String): JEditorPane {
        val toolWindowSize = toolWindow.toolWindow.component.size
        val docPanel = JEditorPane().apply {
            contentType = "text/html"
            text = doc
        }
        // It took me lots of time to handle the size...
        // it turns out that the preferredSize should not be overridden or set at the beginning
        // it should be called first to get some internal logic (quite complicated seems)
        docPanel.size = Dimension(toolWindowSize.width * 8 / 10, Short.MAX_VALUE.toInt())
        val result = docPanel.preferredSize
        docPanel.preferredSize = Dimension(toolWindowSize.width * 8 / 10, result.height)
        return docPanel
    }

    fun createExprPanel(typeAndExpr: String): EditorEx {
        val editor = toolWindow.createEditor()
        editor.document.setText(typeAndExpr)
        // TODO DRY
        val toolWindowSize = toolWindow.toolWindow.component.size
        editor.component.size = Dimension(toolWindowSize.width * 8 / 10, Short.MAX_VALUE.toInt())
        val result = editor.component.preferredSize
        editor.component.preferredSize = Dimension(toolWindowSize.width * 8 / 10, result.height)
        return editor
    }

    fun createPopupPanel(typeAndExpr: String, doc: String) {
        val typeAndExpr = createExprPanel(typeAndExpr)
        val docPanel = createDocPanel(doc)
        val jPanel = JPanel(VerticalLayout(1))
        jPanel.add(typeAndExpr.component)
        jPanel.add(docPanel)
        val popup = JBScrollPane(jPanel)
        popup.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        popup.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

        val factory = JBPopupFactory.getInstance()
        factory.createComponentPopupBuilder(popup, popup)
            // .setTitle(title)
            // .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()
            // .showInBestPositionFor(editor)
            // .showInCenterOf(toolWindow.component)
            // .showInFocusCenter()
            .show(factory.guessBestPopupLocation(toolWindow.toolWindow.component))

    }

}