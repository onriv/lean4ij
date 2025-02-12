/**
 * For os process related utilities
 * TODO for intellij idea related stuff it should
 *      have some other ns like `intellij` or `idea`
 */
package lean4ij.util

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

fun String.executeAt(workingDir: File): String {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText() + "\n" + proc.errorStream.bufferedReader().readText()
    } catch (e: IOException) {
        throw e
    }
}