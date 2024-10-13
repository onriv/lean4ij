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
import com.intellij.ui.scale.JBUIScale.scale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lean4ij.Lean4Settings
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
import javax.swing.JTextPane
import javax.swing.ScrollPaneConstants

/**
 * Since currently we don't have a language implementation for the infoview, we cannot hover the content directly. Hence, we here implement a custom
 * hovering logic for the infoview on infoview. Other reason is the vscode version infoview can hover on doc again and recursively. This is not supported by
 * intellij idea (although intellij idea can open multiple docs in the documentation tool window, but ti's some kind not the same)
 * TODO this is still very wrong, check [com.intellij.codeInsight.documentation.DocumentationScrollPane.setViewportView]
 */
class InfoviewPopupEditorPane(text: String, maxWidth: Int, maxHeight: Int) : JTextPane() {

    private val lean4Settings = service<Lean4Settings>()

    init {
        val scheme = EditorColorsManager.getInstance().globalScheme
        val schemeFont = scheme.getFont(EditorFontType.PLAIN)
        contentType = "text/html"
        // must add this, ref: https://stackoverflow.com/questions/12542733/setting-default-font-in-jeditorpane
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        // TODO maybe some setting for this, font/size etc
        font = schemeFont
        this.text = text
        val width = getPreferredContentWidth(text.length, preferredSize)
        val height = getPreferredHeightByWidth(width)
        preferredSize = Dimension(width, height)
    }

    /**
     * code copied from [com.intellij.codeInsight.documentation.DocumentationEditorPane.getPreferredContentWidth]
     */
    private fun getPreferredContentWidth(textLength: Int, preferredSize: Dimension): Int {
        // Heuristics to calculate popup width based on the amount of the content.
        // The proportions are set for 4 chars/1px in range between 200 and 1000 chars.
        // 200 chars and less is 300px, 1000 chars and more is 500px.
        // These values were calculated based on experiments with varied content and manual resizing to comfortable width.
        val width1 = lean4Settings.nativeInfoviewPopupMinWidthTextLengthUpperBound
        val width2 = lean4Settings.nativeInfoviewPopupMaxWidthTextLengthLowerBound
        val minWidth = lean4Settings.nativeInfoviewPopupPreferredMinWidth
        val maxWidth = lean4Settings.nativeInfoviewPopupPreferredMaxWidth
        val contentLengthPreferredSize = if (textLength < width1) {
            minWidth
        } else if (textLength in (width1 + 1) until width2) {
            minWidth + (textLength - width1) * (maxWidth - minWidth) / (width2 - width1)
        } else {
            maxWidth
        }
        return scale(contentLengthPreferredSize)
    }

    private var myCachedPreferredSize: Dimension? = null

    /**
     * code copied from [com.intellij.codeInsight.documentation.DocumentationEditorPane.getPreferredHeightByWidth]
     */
    private fun getPreferredHeightByWidth(width: Int): Int {
        if (myCachedPreferredSize != null && myCachedPreferredSize!!.width == width) {
            return myCachedPreferredSize!!.height
        }
        setSize(width, Short.MAX_VALUE.toInt())
        val result = preferredSize
        myCachedPreferredSize = Dimension(width, result.height)
        return myCachedPreferredSize!!.height
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
        val leanProjectService: LeanProjectService = project.service()
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
            val sb = InfoviewRender()
            val typeStr = infoToInteractive.type?.toInfoViewString(sb, null) ?: ""
            val exprStr = infoToInteractive.exprExplicit?.toInfoViewString(sb, null) ?: ""
            var htmlDoc: String? = null
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
                val toolWindowSize = toolWindow.toolWindow.component.size
                // htmlDoc = "<body style='width: 10px'>${htmlDoc}</body>"
            }
            // ref: https://plugins.jetbrains.com/docs/intellij/coroutine-tips-and-tricks.html
            // TODO here must limit the range in EDT
            val doc = if (htmlDoc == null) {
                "$exprStr : $typeStr"
            } else {
                "$exprStr : $typeStr<hr>${htmlDoc}"
            }
            launch(Dispatchers.EDT) {
                createPopupPanel(doc)
            }
        }
    }

    fun createDocPanel(doc: String, i: Int): JEditorPane {
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

    /**
     * TODO check [com.intellij.codeInsight.documentation.DocumentationScrollPane.setViewportView]
     */
    fun createPopupPanel(doc: String) {
        val factory = JBPopupFactory.getInstance()
        val toolWindowSize = toolWindow.toolWindow.component.size
        val docPanel = createDocPanel(doc, toolWindowSize.width * 8 / 10)
        val jPanel = JPanel(VerticalLayout(1))
        jPanel.add(docPanel)
        val popup = JBScrollPane(jPanel)
        popup.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        popup.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED

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