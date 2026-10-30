package lean4ij.infoview

import lean4ij.infoview.external.JcefSupport
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CancellationException

class JcefSupportTest {
    private fun loader(load: () -> Class<*>): ClassLoader = object : ClassLoader(null) {
        override fun loadClass(name: String): Class<*> {
            check(name == "com.intellij.ui.jcef.JBCefApp")
            return load()
        }
    }

    @Test
    fun missingPlatformClassIsUnsupported() {
        assertFalse(JcefSupport.isSupported(loader { throw ClassNotFoundException() }))
    }

    @Test
    fun missingRuntimeDependencyIsUnsupported() {
        assertFalse(JcefSupport.isSupported(loader { throw NoClassDefFoundError("org/cef/CefApp") }))
    }

    @Test
    fun supportedRuntimeIsAvailable() {
        assertTrue(JcefSupport.isSupported(loader { SupportedApp::class.java }))
    }

    @Test
    fun unsupportedRuntimeIsUnavailable() {
        assertFalse(JcefSupport.isSupported(loader { UnsupportedApp::class.java }))
    }

    @Test
    fun missingDependencyDuringSupportCheckIsUnsupported() {
        assertFalse(JcefSupport.isSupported(loader { MissingRuntimeApp::class.java }))
    }

    @Test(expected = CancellationException::class)
    fun cancellationPropagates() {
        JcefSupport.isSupported(loader { CancelledApp::class.java })
    }

    @Test(expected = IllegalStateException::class)
    fun unexpectedFailurePropagates() {
        JcefSupport.isSupported(loader { BrokenApp::class.java })
    }

    class SupportedApp {
        companion object {
            @JvmStatic fun isSupported() = true
        }
    }

    class UnsupportedApp {
        companion object {
            @JvmStatic fun isSupported() = false
        }
    }

    class MissingRuntimeApp {
        companion object {
            @JvmStatic fun isSupported(): Boolean = throw NoClassDefFoundError("org/cef/CefApp")
        }
    }

    class CancelledApp {
        companion object {
            @JvmStatic fun isSupported(): Boolean = throw CancellationException()
        }
    }

    class BrokenApp {
        companion object {
            @JvmStatic fun isSupported(): Boolean = error("Unexpected failure")
        }
    }
}
