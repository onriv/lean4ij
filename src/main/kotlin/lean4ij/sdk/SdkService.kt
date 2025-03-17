package lean4ij.sdk

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import lean4ij.language.Lean4SdkType
import lean4ij.project.ToolchainService
import lean4ij.util.notifyErr
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

@Service(Service.Level.PROJECT)
class SdkService(private val project: Project) {

    /**
     * TODO this is duplicated with [lean4ij.lsp.LeanLanguageServerProvider.setServerCommand]
     */
    fun getLeanVersion(): String? {
        val toolchainFile = ToolchainService.expectedToolchainPath(project);
        if (!toolchainFile.exists()) {
            // error only shown
            // if they open a lean file
            // see LeanFileOpenedListener
            return null
        }
        if (!toolchainFile.isRegularFile()) {
            val content =
                "File $toolchainFile lean-toolchain is not a regular file. Please check if the project is setup correctly"
            project.notifyErr(content)
            return null
        }
        val toolchain = toolchainFile.toFile().readText().trim()
        return toolchain
    }

    /**
     * TODO this is duplicated with [lean4ij.lsp.LeanLanguageServerProvider.setServerCommand]
     */
    fun getHomePath(toolchain: String): Path? {
        val toolchainDir = toolchain.replace("/", "--").replace(":", "---")
        // toolchain path is $HOME/.elan/toolchains/<toolchainDir>
        val toolchainPath = Path.of(System.getProperty("user.home"), ".elan", "toolchains", toolchainDir)
        if (!toolchainPath.exists()) {
            val content = "Path $toolchainPath does not exist. Please try to setup the toolchain outside the IDE."
            project.notifyErr(content)
            return null
        }
        if (!toolchainPath.isDirectory()) {
            val content = "Path $toolchainPath is not a directory. Please check if the toolchain setup correctly."
            project.notifyErr(content)
            return null
        }
        return toolchainPath
    }

    fun setupModule() {
        val application = ApplicationManager.getApplication()
        val toolchain = getLeanVersion() ?: return
        val toolchainPath = getHomePath(toolchain)?.toString() ?: return
        val sdkName = toolchain.split('/').last().replace(':', ' ')
        var sdk: Sdk? = ProjectJdkTable.getInstance().findJdk(sdkName)
        val sdkCompletable = CompletableFuture<Sdk>()
        if (sdk == null) {
            application.invokeLater {
                application.runWriteAction {
                    ProjectJdkTable.getInstance().run {
                        val newSdk = ProjectJdkImpl(sdkName, Lean4SdkType.INSTANCE)
                        newSdk.sdkModificator.run {
                            // TODO showing homePath in external library seems cumbersome
                            // homePath = toolchainPath
                            addRoot(
                                VfsUtil.findFile(Path.of(toolchainPath, "src", "lean"), true)!!,
                                OrderRootType.CLASSES
                            )
                            commitChanges()
                        }
                        addJdk(newSdk)
                        sdkCompletable.complete(newSdk)
                    }
                }
            }
        } else {
            sdkCompletable.complete(sdk)
        }

        // TODO maybe it's not a good way doing it synchronously
        // TODO It indeed can be ran out of time
        try {
            sdk = sdkCompletable.get(20, TimeUnit.SECONDS)
            project.basePath?.let { basePath ->
                thisLogger().info("current module is $basePath")
                project.modules.singleOrNull()?.let {
                    ModuleRootModificationUtil.updateModel(it) { rootModule ->
                        rootModule.contentEntries.singleOrNull()?.run {
                            rootModule.sdk = sdk
                            val projectRootManager = ProjectRootManager.getInstance(project)
                            application.invokeLater {
                                application.runWriteAction {
                                    projectRootManager.projectSdk = sdk
                                    projectRootManager.setProjectSdkName(toolchain, "Lean4")
                                }
                            }
                            val lakePath = Paths.get(basePath, ".lake")
                            // This path can be not exist for the first setup, in the case we skip adding it to exclude folder
                            if (lakePath.exists()) {
                                addExcludeFolder(
                                    VfsUtil.findFile(
                                        Paths.get(basePath, ".lake"),
                                        true
                                    )!!
                                )
                            }
                        }
                    }
                    project.save()
                }
            }
        } catch (e: TimeoutException) {
            project.notifyErr("Timeout for setting uyp the sdk for $toolchain")
            thisLogger().error(e)
        }
    }
}
