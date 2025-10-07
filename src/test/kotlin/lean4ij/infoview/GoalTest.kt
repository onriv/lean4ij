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
/**
 * Running this test throws error:
 * TODO it maybe caused by using ktor
at com.intellij.ide.startup.impl.StartupManagerImplKt$launchActivity$1.invokeSuspend(StartupManagerImpl.kt:496)
Caused by: java.lang.NoSuchMethodError: 'java.lang.Object kotlinx.coroutines.CancellableContinuation.tryResume(java.lang.Object, java.lang.Object, kotlin.jvm.functions.Function1)'
at com.intellij.core.rwmutex.cqs.CancellableQueueSynchronizer.tryResumeImpl(CancellableQueueSynchronizer.kt:378)
at com.intellij.core.rwmutex.cqs.CancellableQueueSynchronizer.resume(CancellableQueueSynchronizer.kt:283)
at com.intellij.core.rwmutex.ReadWriteMutexWithWriteIntentImpl.releaseReadPermit(ReadWriteMutexWithWriteIntent.kt:203)
at com.intellij.core.rwmutex.ReadPermitImpl.release(RWMutexIdea.kt:171)
at com.intellij.openapi.application.impl.ThreadState.release(AnyThreadWriteThreadingSupport.kt:43)
at com.intellij.openapi.application.impl.AnyThreadWriteThreadingSupport.tryRunReadAction(AnyThreadWriteThreadingSupport.kt:357)
at com.intellij.openapi.application.impl.ApplicationImpl.tryRunReadAction(ApplicationImpl.java:971)
at com.intellij.openapi.application.rw.CancellableReadActionKt.cancellableReadActionInternal$lambda$3$lambda$2(cancellableReadAction.kt:30)
at com.intellij.openapi.progress.util.ProgressIndicatorUtilService.runActionAndCancelBeforeWrite(ProgressIndicatorUtilService.java:66)
at com.intellij.openapi.progress.util.ProgressIndicatorUtils.runActionAndCancelBeforeWrite(ProgressIndicatorUtils.java:157)
 */
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
