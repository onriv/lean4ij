package com.github.onriv.ijpluginlean.util

import com.github.onriv.ijpluginlean.util.LeanUtil.runCommand
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase

class LeanUtilTest : TestCase() {

    fun testRunCommand() {
        val curlVersionOutput = runCommand("curl --version")
        println(curlVersionOutput)
    }

}