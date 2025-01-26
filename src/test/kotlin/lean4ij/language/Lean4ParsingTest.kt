package lean4ij.language

import com.intellij.testFramework.ParsingTestCase

/**
 * check https://plugins.jetbrains.com/docs/intellij/parsing-test.html#define-a-parsing-test
 * for how the skeleton for the test class
 *
 * the TLDR way to creating parsing related test:
 * 1. add a method with name like `test<SomeSpec>`, for example test<ParsingDefinition>, and add `doTest(true)` to its body
 * 2. add a lean file like `<SomeSpec>.lean`
 * 3. run the test, it will fail with message like `junit.framework.AssertionFailedError: No output text found. File src/test/testData/<SomeSpec>.txt created`
 * 4. run it again, adn use it as regression test in future development.
 */
class Lean4ParsingTest : ParsingTestCase("", "lean", Lean4ParserDefinition()) {

    fun testParsingDefinition() {
        doTest(true)
    }

    /**
     * @return path to test data file directory relative to root of this module.
     */
    override fun getTestDataPath(): String {
        return "src/test/testData"
    }

    override fun includeRanges(): Boolean {
        return true
    }

}