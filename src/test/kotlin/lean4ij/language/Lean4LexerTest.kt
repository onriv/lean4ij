package lean4ij.language

import com.intellij.lexer.Lexer
import com.intellij.testFramework.LexerTestCase
import lean4ij.language.psi.TokenType

/**
 * Term-level keywords (`let`, `if`, `do`, ...) must be lexed as keywords, while identifiers that merely start
 * with a keyword (`letter`, `info`, `dot`) must stay identifiers (longest match).
 */
class Lean4LexerTest : LexerTestCase() {

    override fun createLexer(): Lexer = Lean4LexerAdapter()

    override fun getDirPath(): String = "src/test/testData"

    private fun tokenTypes(text: String): List<String> {
        val lexer = createLexer()
        lexer.start(text)
        val result = mutableListOf<String>()
        while (lexer.tokenType != null) {
            if (lexer.tokenType != TokenType.WHITE_SPACE) {
                result.add("${lexer.tokenType}:${lexer.tokenText}")
            }
            lexer.advance()
        }
        return result
    }

    fun testLetIsKeyword() {
        assertEquals(
            listOf("KEYWORD_COMMAND6:let", "IDENTIFIER:x", "ASSIGN::=", "NUMBER:1"),
            tokenTypes("let x := 1")
        )
    }

    fun testTermKeywords() {
        for (kw in listOf("let", "if", "then", "else", "do", "return", "show", "from", "calc", "fun", "match", "have")) {
            assertEquals(kw, listOf("KEYWORD_COMMAND6:$kw"), tokenTypes(kw))
        }
    }

    fun testIdentifiersStartingWithKeyword() {
        for (id in listOf("letter", "info", "dot", "iff", "thenX", "let'", "do_it", "fromJson")) {
            assertEquals(id, listOf("IDENTIFIER:$id"), tokenTypes(id))
        }
    }

    fun testVariableIsKeyword() {
        assertEquals(listOf("KEYWORD_COMMAND4:variable"), tokenTypes("variable"))
    }
}
