package lean4ij.infoview.external

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import lean4ij.util.leanProjectScope
import lean4ij.util.notify
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefAuthCallback
import org.cef.callback.CefCallback
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefRequestHandler
import org.cef.handler.CefResourceRequestHandler
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import org.cef.security.CefSSLInfo
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.event.DocumentEvent

/**
 * TODO the name like infoview and infoView is inconsistent in the whole codebase...
 */
@Service(Service.Level.PROJECT)
class JcefInfoviewService(private val project: Project) {
    /**
     * Still cannot add case-sensitive, up and down search
     * - https://github.com/JetBrains/intellij-plugins/blob/master/qodana/core/src/org/jetbrains/qodana/ui/link/LinkCloudProjectView.kt
     * - https://github.com/plaskowski/EmbeddedBrowserIntellijPlugin/blob/main/src/main/kotlin/com/github/plaskowski/embeddedbrowserintellijplugin/ui/TextFieldAction.kt
     *
     * may help
     */
    val searchTextField : SearchTextField = SearchTextField()
    val searchTextFlow: Channel<String> = Channel()

    init {
        searchTextField.isVisible = false
        searchTextField.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                project.leanProjectScope.launch {
                    searchTextFlow.send(searchTextField.text)
                }
            }
        })
        searchTextField.addKeyboardListener(object : KeyAdapter() {
            private fun searchDown() {
                val text = searchTextField.text
                if (text.isNotEmpty()) {
                    browser?.cefBrowser?.find(text, true, false, true)
                }
            }

            private fun searchUp() {
                val text = searchTextField.text
                if (text.isNotEmpty()) {
                    browser?.cefBrowser?.find(text, false, false, true)
                }
                return
            }

            override fun keyReleased(e: KeyEvent) {
                when {
                    e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown -> searchDown()
                    e.keyCode == KeyEvent.VK_DOWN -> searchDown()
                    e.keyCode == KeyEvent.VK_ENTER && e.isShiftDown -> searchUp()
                    e.keyCode == KeyEvent.VK_UP -> searchUp()
                    e.keyCode == KeyEvent.VK_ESCAPE -> {
                        searchTextField.isVisible = false
                        project.leanProjectScope.launch {
                            searchTextFlow.send("")
                        }
                    }
                }
            }
        })
        project.leanProjectScope.launch {
            // TODO should this be stopped sometime? like many other infinite loops
            while (true) {
                val text = searchTextFlow.receive()
                if (text.isEmpty()) {
                    browser?.cefBrowser?.stopFinding(true)
                } else {
                    browser?.cefBrowser?.find(text, true, false, false)
                }
            }
        }
    }

    var actionToolbar: ActionToolbar? = null
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

        // handle link clicks, which should be opened in real browser but not jcef

        // browser.cefBrowser.find
        browser.jbCefClient
            .addRequestHandler(object : CefRequestHandler {
                override fun onBeforeBrowse(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    request: CefRequest?,
                    user_gesture: Boolean,
                    is_redirect: Boolean
                ): Boolean {
                    if (request == null) return false
                    val isInfoview = request.url.startsWith(_url!!)
                    if (isInfoview) {
                        return false
                    } else {
                        BrowserUtil.browse(request.url)
                        return true
                    }
                }

                override fun onOpenURLFromTab(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    target_url: String?,
                    user_gesture: Boolean
                ): Boolean {
                    return false
                }

                override fun getResourceRequestHandler(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    request: CefRequest?,
                    isNavigation: Boolean,
                    isDownload: Boolean,
                    requestInitiator: String?,
                    disableDefaultHandling: BoolRef?
                ): CefResourceRequestHandler? {
                    return null
                }

                override fun getAuthCredentials(
                    browser: CefBrowser?,
                    origin_url: String?,
                    isProxy: Boolean,
                    host: String?,
                    port: Int,
                    realm: String?,
                    scheme: String?,
                    callback: CefAuthCallback?
                ): Boolean {
                    return false
                }

                override fun onCertificateError(
                    browser: CefBrowser?,
                    cert_error: CefLoadHandler.ErrorCode?,
                    request_url: String?,
                    sslInfo: CefSSLInfo?,
                    callback: CefCallback?
                ): Boolean {
                    return false
                }

                override fun onRenderProcessTerminated(
                    browser: CefBrowser?,
                    status: CefRequestHandler.TerminationStatus?
                ) {
                }
            }, browser.cefBrowser )
        browser
    } else {
        // TODO make this shorter
        thisLogger().error(errMsg)
        null
    }
}