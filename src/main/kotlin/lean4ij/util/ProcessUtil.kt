/**
 * For os process related utilities
 * TODO for intellij idea related stuff it should
 *      have some other ns like `intellij` or `idea`
 */
package lean4ij.util

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * TODO the api seems not in a good design here, the concrete command should not be the subject
 */
fun String.execute(workingDir: File, environments: Map<String, String> = mapOf()): String {
    try {
        val parts = this.split("\\s".toRegex())
        val processBuilder = ProcessBuilder(*parts.toTypedArray())
        // set environments
        processBuilder.environment().putAll(environments)
        val proc = processBuilder
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