package lean4ij.infoview// TODO removed for using internal api:

import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import kotlinx.coroutines.launch
import lean4ij.lsp.data.CodeWithInfosTag
import lean4ij.lsp.data.InteractiveInfoParams
import lean4ij.lsp.data.Position
import lean4ij.project.LeanProjectService
import lean4ij.util.LspUtil
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.awt.Dimension
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants


/**
 * TODO remove the internal api used here: DocumentationHtmlUtil.getDocPopupPreferredMinWidth()
 */
class CodeWithInfosDocumentationHyperLink(
    val toolWindow: LeanInfoViewWindow,
    val file: VirtualFile,
    val logicalPosition: LogicalPosition,
    val codeWithInfosTag: CodeWithInfosTag,
    val point: RelativePoint
) : HyperlinkInfo {
    override fun navigate(project: Project) {
        val leanProjectService : LeanProjectService = project.service()
        leanProjectService.scope.launch {
            val session = leanProjectService.file(file).getSession()
            // file.url has format file://I:/.. whereas file.path has format "I:/..." in windows
            // TODO absolutely the different formats for url/uri/path should be summarize somewhere
            val textDocument = TextDocumentIdentifier(LspUtil.quote(file.path))
            val logicalPosition = logicalPosition
            val position = Position(line = logicalPosition.line, character = logicalPosition.column)
            val rpcParams = InteractiveInfoParams(
                sessionId = session,
                params = codeWithInfosTag.f0.info,
                textDocument = textDocument,
                position = position
            )
            val infoToInteractive = leanProjectService.languageServer.await()
                .infoToInteractive(rpcParams)
            var htmlDoc : String? = null
            if (infoToInteractive.doc != null) {
                val markdownDoc: String = infoToInteractive.doc
                val flavour = CommonMarkFlavourDescriptor()
                val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownDoc)
                htmlDoc = HtmlGenerator(markdownDoc, parsedTree, flavour).generateHtml()
            }
            val typeStr = infoToInteractive.type?.toInfoViewString(0, null) ?: ""
            val exprStr = infoToInteractive.exprExplicit?.toInfoViewString(0, null) ?: ""
            ApplicationManager.getApplication().invokeLater {
                createPopupPanel("$exprStr : $typeStr", htmlDoc)
            }
        }
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
        // TODO this uses internal ai
        // val width = Math.min(getPreferredContentWidth(doc.length), maxWidth)
        val width = maxWidth
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
        // TODO this uses internal ai
        // val width = Math.min(getPreferredContentWidth(doc.length), maxWidth)
        val width = maxWidth
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
     * TODO this method use internal api
     */
    // private fun getPreferredContentWidth(textLength: Int): Int {
    //     // Heuristics to calculate popup width based on the amount of the content.
    //     // The proportions are set for 4 chars/1px in range between 200 and 1000 chars.
    //     // 200 chars and less is 300px, 1000 chars and more is 500px.
    //     // These values were calculated based on experiments with varied content and manual resizing to comfortable width.
    //     val contentLengthPreferredSize = if (textLength < 200) {
    //         docPopupPreferredMinWidth
    //     } else if (textLength > 200 && textLength < 1000) {
    //         docPopupPreferredMinWidth +
    //                 (textLength - 200) * (docPopupPreferredMaxWidth - docPopupPreferredMinWidth) / (1000 - 200)
    //     } else {
    //         docPopupPreferredMaxWidth
    //     }
    //     return scale(contentLengthPreferredSize)
    // }
}