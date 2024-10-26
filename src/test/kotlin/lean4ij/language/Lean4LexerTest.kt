package lean4ij.language

import com.google.common.io.Resources
import com.intellij.lexer.FlexAdapter
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class Lean4LexerTest {

    @Test
    fun testLexer() {

        Resources.getResource("todo").openStream().use {
            InputStreamReader(it).use {
                BufferedReader(it).use {
                    val lean4Lexer = Lean4Lexer(it)
                    var advance = lean4Lexer.advance()
                    advance.language
                }
            }
        }


    }

}