package lean4ij.infoview.external

import java.lang.reflect.InvocationTargetException

object JcefSupport {
    val isSupported: Boolean by lazy { isSupported(JcefSupport::class.java.classLoader) }

    // JCEF moved to an optional bundled plugin in IntelliJ 2026.2.
    internal fun isSupported(classLoader: ClassLoader): Boolean {
        return try {
            val app = classLoader.loadClass("com.intellij.ui.jcef.JBCefApp")
            app.getMethod("isSupported").invoke(null) as Boolean
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: LinkageError) {
            false
        } catch (e: InvocationTargetException) {
            if (e.cause is LinkageError) false else throw e.targetException
        }
    }
}
