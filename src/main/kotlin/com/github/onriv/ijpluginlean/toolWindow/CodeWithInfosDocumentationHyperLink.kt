package com.github.onriv.ijpluginlean.toolWindow

import com.github.onriv.ijpluginlean.lsp.LeanLspServerManager
import com.github.onriv.ijpluginlean.lsp.data.CodeWithInfos
import com.github.onriv.ijpluginlean.lsp.data.CodeWithInfosTag
// import com.github.onriv.ijpluginlean.lsp.data.gson
import com.intellij.codeInsight.documentation.DocumentationHtmlUtil.docPopupPreferredMaxWidth
import com.intellij.codeInsight.documentation.DocumentationHtmlUtil.docPopupPreferredMinWidth
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.scale.JBUIScale.scale
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.awt.Dimension
import java.awt.Point
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

class CodeWithInfosDocumentationHyperLink(
    val toolWindow: LeanInfoViewWindowFactory.LeanInfoViewWindow,
    val file: VirtualFile,
    val caret: Caret,
    val codeWithInfosTag: CodeWithInfosTag,
    val point: RelativePoint
) : HyperlinkInfo {
    override fun navigate(project: Project) {
        // var infoToInteractive =
        //     LeanLspServerManager.getInstance(project)
        //         .infoToInteractive(file, caret, codeWithInfosTag.f0.info) as Map<String, Any>
        // val type: CodeWithInfos = gson.fromJson(gson.toJson(infoToInteractive["type"]), CodeWithInfos::class.java)
        // val exprExplicit: CodeWithInfos =
        //     gson.fromJson(gson.toJson(infoToInteractive["exprExplicit"]), CodeWithInfos::class.java)
        // var htmlDoc : String? = null
        // if (infoToInteractive["doc"] != null) {
        //     val markdownDoc: String = infoToInteractive["doc"] as String
        //     val flavour = CommonMarkFlavourDescriptor()
        //     val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownDoc)
        //     htmlDoc = HtmlGenerator(markdownDoc, parsedTree, flavour).generateHtml()
        // }
        // val typeStr = type.toInfoViewString(0, null)
        // val exprStr = exprExplicit.toInfoViewString(0, null)
        // createPopupPanel("$exprStr : $typeStr", htmlDoc)
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
        val maxWidth = toolWindowSize.width * 8 / 10
        val width = Math.min(getPreferredContentWidth(doc.length), maxWidth)
        docPanel.size = Dimension(width, Short.MAX_VALUE.toInt())
        val result = docPanel.preferredSize
        docPanel.preferredSize = Dimension(width, result.height)
        return docPanel
    }

    fun createExprPanel(typeAndExpr: String): EditorEx {
        val editor = toolWindow.createEditor()
        editor.document.setText(typeAndExpr)
        // TODO DRY
        val toolWindowSize = toolWindow.toolWindow.component.size

        val maxWidth = toolWindowSize.width * 8 / 10
        val width = Math.min(getPreferredContentWidth(typeAndExpr.length), maxWidth)
        editor.component.size = Dimension(width, Short.MAX_VALUE.toInt())
        val result = editor.component.preferredSize
        editor.component.preferredSize = Dimension(width, result.height)
        return editor
    }

    fun createPopupPanel(typeAndExpr: String, doc: String?) {
        val factory = JBPopupFactory.getInstance()
        val typeAndExprPanel = createExprPanel(typeAndExpr)
        val jPanel = JPanel(VerticalLayout(1))
        jPanel.add(typeAndExprPanel.component)
        if (doc != null) {
            val docPanel = createDocPanel(doc)
            jPanel.add(docPanel)
        }
        val popup = JBScrollPane(jPanel)
        popup.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        popup.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

        factory.createComponentPopupBuilder(popup, popup)
            // .setTitle(title)
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()
            .show(point)
            // .showInScreenCoordinates(toolWindow.toolWindow.component, point)
            // .showInBestPositionFor(editor)
            // .showInCenterOf(toolWindow.component)
            // .showInFocusCenter()
            // .show(factory.guessBestPopupLocation(toolWindow.toolWindow.component))

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