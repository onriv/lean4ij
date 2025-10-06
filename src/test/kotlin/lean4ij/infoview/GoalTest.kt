package lean4ij.infoview

import com.google.common.io.Resources
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import lean4ij.lsp.LeanLanguageServer
import lean4ij.lsp.data.MsgEmbed
import lean4ij.lsp.data.TaggedText
import org.junit.Test
import lean4ij.test.readResource
import org.junit.Ignore

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
// Ignore temporally for maybe wrongly setup of project (check if update it to plugin v2 works or not)
@Ignore
class GoalTest : BasePlatformTestCase() {

    @Test
    fun testDeserializeLazyTraceChildrenToInteractiveResp() {
        val s = readResource("lsp/lazyTraceChildrenToInteractive_resp_sample1.json")
        // TODO no sure what happened but it seems it must be seperated into two part
        val r = LeanLanguageServer.gson.fromJson<List<JsonObject>>(s, object : TypeToken<List<JsonObject>>() {}.type)
            .map {
                LeanLanguageServer.gson.fromJson<TaggedText<MsgEmbed>>(it, object : TypeToken<TaggedText<MsgEmbed>>() {}.type)
            }.toList()
        assertEquals(r.size, 5)
        val t = LeanLanguageServer.gson.fromJson<List<TaggedText<MsgEmbed>>>(s, object : TypeToken<List<TaggedText<MsgEmbed>>>(){}.type)
        assertEquals(t.size, 5)
    }

    @Test
    fun testDeserializeMsgEmbedTrace() {
        val s = readResource("lsp/msgEmbedTrace_sample1.json")
        // TODO no sure what happened but it seems it must be seperated into two part
        val r = LeanLanguageServer.gson.fromJson<MsgEmbed>(s, object : TypeToken<MsgEmbed>() {}.type)
        TestCase.assertNotNull(r)
    }
}
