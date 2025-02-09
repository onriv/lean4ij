package lean4ij.util

import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object LeanUtil {

    fun isLeanFile(file: VirtualFile) : Boolean {
        return file.extension?.let { it == "lean" || it == "lean4"} == true
    }

    fun isLeanFile(url: String) : Boolean {
        return url.endsWith(".lean") || url.endsWith(".lean4")
    }

}