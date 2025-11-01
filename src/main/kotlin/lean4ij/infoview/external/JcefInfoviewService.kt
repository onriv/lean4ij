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
import java.lang.reflect.Proxy
import javax.swing.event.DocumentEvent
import kotlin.reflect.KProperty0

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
    val searchTextField: SearchTextField = SearchTextField()
    val searchTextFlow: Channel<String> = Channel()

    init {
        searchTextField.isVisible = false
        searchTextField.addDocumentListener(object : DocumentAdapter() {
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
            .addRequestHandler(createCefRequestHandler(), browser.cefBrowser)
        browser
    } else {
        // TODO make this shorter
        thisLogger().error(errMsg)
        null
    }

    /**
     * Since version 2025.3, the [CefRequestHandler] class introduces a new method
     * CefRequestHandler#onRenderProcessTerminated(CefBrowser browser, TerminationStatus status, int error_code, String error_string)
     * It makes the verifier failed in version 2025.3 and for versions less than 2025.3 we cannot override it
     * Hence temporally here we use dynamic proxy to solve to problem and until
     * we no longer need to support versions less than 2025.3, we will remove it
     * see https://github.com/JetBrains/intellij-community/blob/master/platform/ui.jcef/jcef/JBCefClient.java#L643 for some information
     * TODO remove this once ready for 2025.3 and no longer need lower version
     *
     */
    fun createCefRequestHandler(): CefRequestHandler {
        val handler = InfoviewCefRequestHandler(::url);
        return Proxy.newProxyInstance(
            CefRequestHandler::class.java.classLoader,
            arrayOf(CefRequestHandler::class.java)
        ) { _, method, args ->
            when (method.name) {
                "onBeforeBrowse" -> {
                    return@newProxyInstance handler.onBeforeBrowse(
                        args[0] as CefBrowser?,
                        args[1] as CefFrame?,
                        args[2] as CefRequest?,
                        args[3] as Boolean, args[4] as Boolean)
                }

                "onOpenURLFromTab" -> false
                "getResourceRequestHandler" -> null
                "getAuthCredentials" -> false
                "onCertificateError" -> false
                "onRenderProcessTerminated" -> Unit
                else -> when (method.returnType.name) {
                    "boolean" -> false
                    "void" -> Unit
                    else -> null
                }
            }
        } as CefRequestHandler
    }
}

/**
 * Since version 2025.3, there is a new method required to override
 * and introduce compatibility problem
 * Temporally we use dynamic for this
 * see https://github.com/JetBrains/intellij-community/blob/master/platform/ui.jcef/jcef/JBCefClient.java#L643 for some information
 */
class InfoviewCefRequestHandler(private val urlProp: KProperty0<String?>) /*: CefRequestHandler*/ {
    /*override*/ fun onBeforeBrowse(
        browser: CefBrowser?,
        frame: CefFrame?,
        request: CefRequest?,
        user_gesture: Boolean,
        is_redirect: Boolean
    ): Boolean {
        if (request == null) return false
        val isInfoview = request.url.startsWith(urlProp.get()!!)
        if (isInfoview) {
            return false
        } else {
            BrowserUtil.browse(request.url)
            return true
        }
    }

    /*override*/ fun onOpenURLFromTab(
        browser: CefBrowser?,
        frame: CefFrame?,
        target_url: String?,
        user_gesture: Boolean
    ): Boolean {
        return false
    }

    /*override*/ fun getResourceRequestHandler(
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

    /*override*/ fun getAuthCredentials(
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

    /*override*/ fun onCertificateError(
        browser: CefBrowser?,
        cert_error: CefLoadHandler.ErrorCode?,
        request_url: String?,
        sslInfo: CefSSLInfo?,
        callback: CefCallback?
    ): Boolean {
        return false
    }

    /*override*/ fun onRenderProcessTerminated(
        browser: CefBrowser?,
        status: CefRequestHandler.TerminationStatus?
    ) {
    }
}