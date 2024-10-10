package lean4ij.infoview

import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
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
import kotlin.math.min

/**
 * Since currently we don't have a language implementation for the infoview, we cannot hover the content directly. Hence, we here implement a custom
 * hovering logic for the infoview on infoview. Other reason is the vscode version infoview can hover on doc again and recursively. This is not supported by
 * intellij idea (although intellij idea can open multiple docs in the documentation tool window, but ti's some kind not the same)
 * TODO this is still very wrong, check [com.intellij.codeInsight.documentation.DocumentationScrollPane.setViewportView]
 */
class InfoviewPopupEditorPane(text: String, maxWidth: Int, maxHeight: Int) : JEditorPane() {

    init {
        val scheme = EditorColorsManager.getInstance().globalScheme
        val schemeFont = scheme.getFont(EditorFontType.PLAIN)
        contentType = "text/html"
        // must add this, ref: https://stackoverflow.com/questions/12542733/setting-default-font-in-jeditorpane
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        // TODO maybe some setting for this, font/size etc
        font = schemeFont
        this.text = text

        // TODO this is still very wrong...
        // It took me lots of time to handle the size...
        // it turns out that the preferredSize should not be overridden or set at the beginning
        // it should be called first to get some internal logic (quite complicated seems)
        maximumSize = Dimension(maxWidth, Short.MAX_VALUE.toInt())
        // // val width = Math.min(getPreferredContentWidth(doc.length), maxWidth)
        // val width = maxWidth
        // exprPane.size = Dimension(width, Short.MAX_VALUE.toInt())
        val result = preferredSize
        if (result.width > maxWidth) {
            preferredSize = Dimension(maxWidth, min(maxHeight, result.height*(result.width/maxWidth+1)))
        } else {
            preferredSize = Dimension(result.width, result.height)
        }

    }
}

/**
 * TODO this class absolutely need some refactor and a better implementation
 */
class CodeWithInfosDocumentationHyperLink(
    val scope: CoroutineScope,
    val toolWindow: LeanInfoViewWindow,
    val file: VirtualFile,
    val logicalPosition: LogicalPosition,
    val contextInfo: ContextInfo,
    val point: RelativePoint
) : HyperlinkInfo {
    companion object {
        /**
         * For heuristic determining the height of popup expr doc
         * TODO should this be a config?
         */
        private var height: Int? = null
    }
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
        val maxWidth = toolWindowSize.width * 8 / 10
        val maxHeight = toolWindowSize.height * 8 / 10
        val docPanel = InfoviewPopupEditorPane(doc, maxWidth, maxHeight)
        return docPanel
    }

    /**
     * TODO the width is still not the best
     * check com.intellij.codeInsight.documentation.DocumentationEditorPane#getPreferredContentWidth ...
     * or use document directly
     */
    fun createExprPanel(typeAndExpr: String): JEditorPane {
        val toolWindowSize = toolWindow.toolWindow.component.size
        val maxWidth = toolWindowSize.width * 8 / 10
        val maxHeight = toolWindowSize.height * 8 / 10
        var exprPane = InfoviewPopupEditorPane(typeAndExpr, maxWidth, maxHeight)
        return exprPane
    }

     fun createPopupPanel(typeAndExpr: String, doc: String?) {
        val factory = JBPopupFactory.getInstance()
        val typeAndExprPanel = createExprPanel(typeAndExpr)
        val jPanel = JPanel(VerticalLayout(1))
        jPanel.add(typeAndExprPanel)
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
}