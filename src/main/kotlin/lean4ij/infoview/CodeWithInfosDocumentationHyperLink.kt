package lean4ij.infoview// TODO removed for using internal api:

import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lean4ij.lsp.data.ContextInfo
import lean4ij.lsp.data.InfoviewRender
import lean4ij.lsp.data.InteractiveInfoParams
import lean4ij.lsp.data.Position
import lean4ij.project.LeanProjectService
import lean4ij.util.LspUtil
import org.eclipse.lsp4j.TextDocumentIdentifier
import java.awt.Dimension
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants


/**
 * TODO remove the internal api used here: DocumentationHtmlUtil.getDocPopupPreferredMinWidth()
 */
class CodeWithInfosDocumentationHyperLink(
    val scope: CoroutineScope,
    val toolWindow: LeanInfoViewWindow,
    val file: VirtualFile,
    val logicalPosition: LogicalPosition,
    val contextInfo: ContextInfo,
    val point: RelativePoint
) : HyperlinkInfo {

    private var popupPanel: JBPopup? = null

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
                params = contextInfo,
                textDocument = textDocument,
                position = position
            )
            val infoToInteractive = leanProjectService.languageServer.await()
                .infoToInteractive(rpcParams)
            var htmlDoc : String? = null
            if (infoToInteractive.doc != null) {
                val markdownDoc: String = infoToInteractive.doc
                // val flavour = CommonMarkFlavourDescriptor()
                // var flavour = GFMFlavourDescriptor()
                // val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownDoc)
                // htmlDoc = HtmlGenerator(markdownDoc, parsedTree, flavour).generateHtml()
                // TODO no language for lean yet
                htmlDoc = DocMarkdownToHtmlConverter.convert(project, markdownDoc, null)
                if (htmlDoc.startsWith("<body>") && htmlDoc.endsWith("</body>")) {
                    htmlDoc = htmlDoc.substring("<body>".length, htmlDoc.length - "</body>".length)
                }
                if (htmlDoc.startsWith("<p>") && htmlDoc.endsWith("</p>")) {
                    htmlDoc = htmlDoc.substring("<p>".length, htmlDoc.length - "</p>".length)
                }
                if (htmlDoc.startsWith("<p>")) {
                    // try fixing some empty line
                    htmlDoc = htmlDoc.substring("<p>".length)
                }
                // TODO maybe some css for this?
                htmlDoc = "<body>${htmlDoc}</body>"
            }
            val sb = InfoviewRender()
            val typeStr = infoToInteractive.type?.toInfoViewString(sb, null) ?: ""
            val exprStr = infoToInteractive.exprExplicit?.toInfoViewString(sb, null) ?: ""
            // ref: https://plugins.jetbrains.com/docs/intellij/coroutine-tips-and-tricks.html
            // TODO here must limit the range in EDT
            launch(Dispatchers.EDT) {
                createPopupPanel("$exprStr : $typeStr", htmlDoc)
            }
        }
    }

    fun createDocPanel(doc: String): JEditorPane {
        val toolWindowSize = toolWindow.toolWindow.component.size
        val scheme = EditorColorsManager.getInstance().globalScheme
        val schemeFont = scheme.getFont(EditorFontType.PLAIN)
        val docPanel = JEditorPane().apply {
            contentType = "text/html"
            // must add this, ref: https://stackoverflow.com/questions/12542733/setting-default-font-in-jeditorpane
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            // TODO maybe some setting for this, font/size etc
            font = JBFont.regular()
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

    suspend fun createExprPanel(typeAndExpr: String): EditorEx {
        val editor = toolWindow.popupEditor.await()
        editor.document.setText(typeAndExpr)
        // TODO DRY
        val toolWindowSize = toolWindow.toolWindow.component.size

        val maxWidth = toolWindowSize.width * 8 / 10
        // TODO this uses internal api
        // val width = Math.min(getPreferredContentWidth(doc.length), maxWidth)
        val width = maxWidth
        editor.component.size = Dimension(width, Short.MAX_VALUE.toInt())
        val result = editor.component.preferredSize
        editor.component.preferredSize = Dimension(width, result.height)
        editor.setBorder(null)
        return editor
    }

    suspend fun createPopupPanel(typeAndExpr: String, doc: String?) {
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

        popupPanel = factory.createComponentPopupBuilder(popup, popup)
            // .setTitle(title)
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()
            // .showInScreenCoordinates(toolWindow.toolWindow.component, point)
            // .showInBestPositionFor(editor)
            // .showInCenterOf(toolWindow.component)
            // .showInFocusCenter()
            // .show(factory.guessBestPopupLocation(toolWindow.toolWindow.component))
        popupPanel?.show(point)

    }

    fun cancel() {
        popupPanel?.let {
            ApplicationManager.getApplication().invokeLater {
                it.cancel()
            }
        }
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