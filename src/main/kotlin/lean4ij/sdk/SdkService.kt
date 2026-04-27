package lean4ij.sdk

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtil
import lean4ij.language.Lean4SdkType
import lean4ij.project.ToolchainService
import lean4ij.util.notifyErr
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

@Service(Service.Level.PROJECT)
class SdkService(private val project: Project) {

    fun getLeanVersion(): String? {
        return ToolchainService.readToolchain(project)
    }

    fun getHomePath(toolchain: String): Path? {
        val toolchainPath = ToolchainService.resolveToolchainPath(toolchain)
        if (toolchainPath == null) {
            val dirName = ToolchainService.toolchainToDirName(toolchain)
            val expected = Path.of(System.getProperty("user.home"), ".elan", "toolchains", dirName)
            project.notifyErr("Path $expected does not exist. Please try to setup the toolchain outside the IDE.")
            return null
        }
        if (!toolchainPath.isDirectory()) {
            project.notifyErr("Path $toolchainPath is not a directory. Please check if the toolchain setup correctly.")
            return null
        }
        return toolchainPath
    }

    /**
     * Defer SDK/module wiring to a non-modal EDT slot (#133).
     *
     * Both call sites can land in a modal context — the new-project wizard
     * runs inside `runWithModalProgressBlockingInternal`, so doing
     * `ModuleRootModificationUtil.updateModel` from there fires workspace
     * model events whose `assertWriteActionAllowed` rejects the modal
     * modality. Hopping to non-modal waits until the modal closes.
     */
    fun setupModule() {
        val toolchain = getLeanVersion() ?: return
        val toolchainPath = getHomePath(toolchain)?.toString() ?: return
        val sdkName = toolchain.substringAfterLast("/").replace(':', ' ')
        ApplicationManager.getApplication().invokeLater(
            { doSetupModule(toolchain, toolchainPath, sdkName) },
            ModalityState.nonModal(),
            project.disposed,
        )
    }

    private fun doSetupModule(toolchain: String, toolchainPath: String, sdkName: String) {
        val sdk = getOrCreateSdk(sdkName, toolchainPath) ?: return
        val basePath = project.basePath ?: return
        // Use the first module rather than requiring exactly one — IntelliJ projects
        // may have multiple modules (e.g. when opened from existing sources).
        val module = project.modules.firstOrNull()
        if (module == null) {
            thisLogger().warn("No modules found in project, skipping SDK setup")
            return
        }
        try {
            ModuleRootModificationUtil.updateModel(module) { rootModule ->
                rootModule.contentEntries.firstOrNull()?.run {
                    rootModule.sdk = sdk
                    val lakePath = Paths.get(basePath, ".lake")
                    if (lakePath.exists()) {
                        VfsUtil.findFile(lakePath, true)?.let { lakeVFile ->
                            addExcludeFolder(lakeVFile)
                        }
                    }
                }
            }
            ApplicationManager.getApplication().runWriteAction {
                val projectRootManager = ProjectRootManager.getInstance(project)
                projectRootManager.projectSdk = sdk
                projectRootManager.setProjectSdkName(toolchain, "Lean4")
            }
            project.save()
        } catch (e: Exception) {
            thisLogger().error("Failed to setup module SDK", e)
        }
    }

    private fun getOrCreateSdk(sdkName: String, toolchainPath: String): Sdk? {
        val existing = ProjectJdkTable.getInstance().findJdk(sdkName)
        if (existing != null) return existing
        val application = ApplicationManager.getApplication()
        return try {
            application.runWriteAction(Computable<Sdk> {
                val newSdk = ProjectJdkImpl(sdkName, Lean4SdkType.INSTANCE)
                // homePath is essential: IntelliJ uses it for SDK validation,
                // version detection, persistence, and re-association on project reload.
                newSdk.homePath = toolchainPath
                Lean4SdkType.INSTANCE.setupSdkPaths(newSdk)
                ProjectJdkTable.getInstance().addJdk(newSdk)
                newSdk
            })
        } catch (e: Exception) {
            thisLogger().error("Failed to create Lean SDK", e)
            null
        }
    }
}
