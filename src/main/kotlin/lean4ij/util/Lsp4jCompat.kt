package lean4ij.util

import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.jsonrpc.messages.Either

/**
 * Helpers for code that must run against different lsp4j versions.
 *
 * Which lsp4j the plugin sees at runtime depends on the IDE (IU bundles its own) and on LSP4IJ. The plugin
 * supports 2024.2+ (lsp4j 0.2x) as well as 2026.x+ IDEs / LSP4IJ 0.21+ (lsp4j 1.0.0, LSP 3.18), and some
 * signatures changed between them, so those members are accessed reflectively to avoid NoSuchMethodError.
 */
object Lsp4jCompat {

    private val diagnosticGetMessage = Diagnostic::class.java.getMethod("getMessage")

    /**
     * `Diagnostic.getMessage()` returns `String` in lsp4j < 1.0 and `Either<String, MarkupContent>` since 1.0.
     */
    fun messageOf(diagnostic: Diagnostic): String =
        when (val message = diagnosticGetMessage.invoke(diagnostic)) {
            null -> ""
            is String -> message
            is Either<*, *> -> if (message.isLeft) message.left as String else (message.right as MarkupContent).value
            else -> message.toString()
        }
}
