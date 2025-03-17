package lean4ij.util

import junit.framework.TestCase
import java.io.File

class LeanUtilTest : TestCase() {

    fun testRunCommand() {
        val curlVersionOutput = "curl --version".execute(File("."))
        println(curlVersionOutput)
    }

}