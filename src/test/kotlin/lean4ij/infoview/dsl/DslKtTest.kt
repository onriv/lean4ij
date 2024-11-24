package lean4ij.infoview.dsl

import lean4ij.lsp.LeanLanguageServer
import lean4ij.lsp.data.InteractiveGoals
import lean4ij.lsp.fromJson
import lean4ij.test.readResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.StringBuilder


class DslKtTest {
    @Test
    fun testDsl() {
        val info = info {
            fold {
                h3("Tactic state")
                fold {
                    h3("Tactic state")
                }
            }
        }

    }

    @Test
    fun testDslInteractiveGoals() {
        val s : InteractiveGoals = LeanLanguageServer.gson.fromJson(readResource("lsp/interactiveGoals_sample.json"))
        val t = s.toInfoObjectModel()
        assertEquals("CommMonoid M", s.goals[0].hyps[1].type.toInfoObjectModel().toString())
        val expected = """
            Tactic state
            1 goal
            case intro.intro.intro.intro.intro.intro.intro.intro
            M : Type ?u.53752
            inst✝ : CommMonoid M
            N : Submonoid M
            a b c w : M
            hw : w ∈ N
            z : M
            hz : z ∈ N
            h : a * w = b * z
            w' : M
            hw' : w' ∈ N
            z' : M
            hz' : z' ∈ N
            h' : b * w' = c * z'
            ⊢ a * (w * w') = c * (z * z')
        """.trimIndent()
        assertEquals(expected, t.toString())
        // This is not duplicated, it's as designed for checking the toString method
        // can be called multiple time without relying on things like InputStream that
        // only takes effect for once, and for checking the side effect only takes effect
        // at once
        assertEquals(expected, t.toString())
        t.toStringAndEditorActions(StringBuilder(), mutableListOf(), mutableListOf())
        assertEquals(13, t.children[0].children[1].start!!)
    }

}