package lean4ij.language

import com.intellij.openapi.application.PathManager
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

/**
 * from: https://github.com/JetBrains/godot-support/blob/master/rider/src/main/kotlin/com/jetbrains/rider/plugins/godot/textMate/GodotTextMateBundleProvider.kt
 * https://github.com/mallowigi/permify-jetbrains/blob/de27f901228919ce7eab0c37d8045443283fc4eb/src/main/kotlin/com/mallowigi/permify/PermifyTextMateBundleProvider.kt
 * TODO working
 * from https://raw.githubusercontent.com/leanprover/vscode-lean4/master/vscode-lean4/syntaxes/
 */
class TextMateBundleProvider : TextMateBundleProvider {
    private val files = listOf(
        "package.json",
        "lean4.json",
        "lean4-markdown.json"
    )

    override fun getBundles(): List<TextMateBundleProvider.PluginBundle> {
        // try {
            val tmpDir: Path = Files.createTempDirectory(Path.of(PathManager.getTempPath()), "textmate-lean4")

            files.forEach { fileToCopy ->
                val resource: URL? = javaClass.classLoader.getResource("bundles/$fileToCopy")

                resource?.openStream().use { resourceStream ->
                    if (resourceStream != null) {
                        val target: Path = tmpDir.resolve(fileToCopy)
                        Files.createDirectories(target.parent)
                        Files.copy(resourceStream, target)
                    }
                }
            }

            val bundle = TextMateBundleProvider.PluginBundle("Lean4", tmpDir)
            return listOf(bundle)
        // } catch (e: IOException) {
        //    throw RuntimeException(e)
        // }
    }
}
