package com.github.onriv.ijpluginlean.util

import com.github.onriv.ijpluginlean.util.Lean4Util.runCommand
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase

class Lean4UtilTest : TestCase() {

    fun testRunCommand() {
        val curlVersionOutput = runCommand("curl --version")
        println(curlVersionOutput)
    }

}