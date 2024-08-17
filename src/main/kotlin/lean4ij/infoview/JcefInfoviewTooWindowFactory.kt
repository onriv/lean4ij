package lean4ij.infoview

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser

/**
 * TODO the name like infoview and infoView is inconsistent in the whole codebase...
 */
@Service(Service.Level.PROJECT)
class JcefInfoviewService(private val project: Project) {
    private var url : String? = null
    fun loadUrl(url: String) {
        this.url = url
        browser?.loadURL(url)
    }

    fun reload() {
        url?.let{browser?.loadURL(it)}
    }

    val errMsg = "JCEF is unsupported. It maybe the IDE is started with an alternative JDK that does not include JCEF or Its version is not compatible with the running IDE."
    val browser : JBCefBrowser? = if (JBCefApp.isSupported()) {
        JBCefBrowser()
    } else {
        // TODO make this shorter
        thisLogger().error(errMsg)
        null
    }
}

class JcefInfoviewTooWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val jcefInfoview = SimpleToolWindowPanel(true)
        val jcefService = project.service<JcefInfoviewService>()
        val browser = jcefService.browser
        if (browser != null) {
            jcefInfoview.add(browser.component)
        } else {
            jcefInfoview.add(panel {
                row(jcefService.errMsg){}
            })
        }
        val content = ContentFactory.getInstance().createContent(jcefInfoview, null, false)
        toolWindow.contentManager.addContent(content)
    }
}