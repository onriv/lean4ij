package lean4ij.infoview.external

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import lean4ij.util.notify

/**
 * TODO the name like infoview and infoView is inconsistent in the whole codebase...
 */
@Service(Service.Level.PROJECT)
class JcefInfoviewService(private val project: Project) {
    private var _url: String? = null
    val url get() = _url

    fun loadUrl(url: String) {
        this._url = url
        browser?.loadURL(url)
    }


    fun reload() {
        _url?.let { browser?.loadURL(it) }
    }

    fun increaseZoomLevel() {
        if (browser == null) {
            project.notify("browser for the external infoview is null. Please check build toolwindow for if the external infoview service starts or not.")
            return
        }
        browser.zoomLevel += 0.05
    }

    fun decreaseZoomLevel() {
        if (browser == null) {
            project.notify("browser for the external infoview is null. Please check build toolwindow for if the external infoview service starts or not.")
            return
        }
        browser.zoomLevel -= 0.05
    }

    fun resetZoomLevel() {
        if (browser == null) {
            project.notify("browser for the external infoview is null. Please check build toolwindow for if the external infoview service starts or not.")
            return
        }
        browser.zoomLevel = defaultZoomLevel!!
    }
    var defaultZoomLevel: Double? = null

    val errMsg =
        "JCEF is unsupported. It maybe the IDE is started with an alternative JDK that does not include JCEF or Its version is not compatible with the running IDE."
    val browser: JBCefBrowser? = if (JBCefApp.isSupported()) {
        val browser = JBCefBrowser()
        defaultZoomLevel = browser.zoomLevel
        browser
    } else {
        // TODO make this shorter
        thisLogger().error(errMsg)
        null
    }
}

class JcefInfoviewTooWindowFactory : ToolWindowFactory {

    companion object {
        /**
         * The id is defined in plugin.xml
         */
        fun getToolWindow(project: Project): ToolWindow? =
            ToolWindowManager.getInstance(project).getToolWindow("LeanInfoviewJcef")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val jcefInfoview = SimpleToolWindowPanel(true)
        val jcefService = project.service<JcefInfoviewService>()
        val browser = jcefService.browser
        if (browser != null) {
            jcefInfoview.add(browser.component)
        } else {
            jcefInfoview.add(panel {
                row(jcefService.errMsg) {}
            })
        }
        val content = ContentFactory.getInstance().createContent(jcefInfoview, null, false)
        toolWindow.contentManager.addContent(content)
    }
}