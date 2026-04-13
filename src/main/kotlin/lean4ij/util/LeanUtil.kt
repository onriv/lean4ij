package lean4ij.util

import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Utility functions for identifying Lean source files.
 *
 * Lean files use the `.lean` extension (Lean 4) or `.lean4` extension (less common variant).
 * These checks are used throughout the plugin to filter events and actions to Lean-relevant contexts.
 */
object LeanUtil {

    /** Checks if a [VirtualFile] is a Lean source file based on its extension. */
    fun isLeanFile(file: VirtualFile) : Boolean {
        return file.extension?.let { it == "lean" || it == "lean4"} == true
    }

    /** Checks if a URL/path string ends with a Lean file extension. */
    fun isLeanFile(url: String) : Boolean {
        return url.endsWith(".lean") || url.endsWith(".lean4")
    }

}