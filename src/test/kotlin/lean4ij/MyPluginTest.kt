package lean4ij

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.components.service
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import junit.framework.TestCase
import lean4ij.services.MyProjectService
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
class MyPluginTest : BasePlatformTestCase() {

    fun testXMLFile() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, "<foo>bar</foo>")
        val xmlFile = assertInstanceOf(psiFile, XmlFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, xmlFile.virtualFile))

        assertNotNull(xmlFile.rootTag)

        xmlFile.rootTag?.let {
            assertEquals("foo", it.name)
            assertEquals("bar", it.value.text)
        }
    }

    fun testRename() {
        myFixture.testRename("foo.xml", "foo_after.xml", "a2")
    }

    fun testProjectService() {
        val projectService = project.service<MyProjectService>()
        assertNotNull(projectService)
    }

    override fun getTestDataPath() = "src/test/testData/rename"
}
