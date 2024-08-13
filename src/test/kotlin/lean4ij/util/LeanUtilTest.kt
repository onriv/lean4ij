package lean4ij.util

import lean4ij.util.LeanUtil.runCommand
import junit.framework.TestCase

class LeanUtilTest : TestCase() {

    fun testRunCommand() {
        val curlVersionOutput = runCommand("curl --version")
        println(curlVersionOutput)
    }

}