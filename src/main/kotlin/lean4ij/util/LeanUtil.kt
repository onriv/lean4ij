package lean4ij.util

import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object LeanUtil {
    // TODO should not in here
    fun runCommand(command: String, workingDir: File): String? {
        try {
            val parts = command.split("\\s".toRegex())
            val process = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            process.waitFor(60, TimeUnit.MINUTES)
            return process.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    // TODO should no in here
    fun runCommand(command: String): String? {
        try {
            val process = Runtime.getRuntime().exec(command)
            process.waitFor(60, TimeUnit.MINUTES)
            return process.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun isLeanFile(file: VirtualFile) : Boolean {
        return file.extension?.let { it == "lean" || it == "lean4"} ?: false
    }

    fun isLeanFile(url: String) : Boolean {
        return url.endsWith(".lean") || url.endsWith(".lean4")
    }

}