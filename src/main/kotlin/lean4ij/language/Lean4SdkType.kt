package lean4ij.language

import com.intellij.openapi.projectRoots.AdditionalDataConfigurable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.projectRoots.SdkModel
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import org.jdom.Element
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

class Lean4SdkType : SdkType("Lean4") {

    companion object {
        val INSTANCE = Lean4SdkType()
        private const val LEAN_BIN = "bin/lean"
        private const val SRC_LEAN = "src/lean"
    }

    override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {
    }

    /**
     * Suggest the default elan toolchains directory so users can browse installed toolchains.
     */
    override fun suggestHomePath(): String? {
        val elanToolchains = Path.of(System.getProperty("user.home"), ".elan", "toolchains")
        return if (elanToolchains.isDirectory()) elanToolchains.toString() else null
    }

    override fun suggestHomePaths(): MutableCollection<String> {
        val elanToolchains = Path.of(System.getProperty("user.home"), ".elan", "toolchains")
        if (!elanToolchains.isDirectory()) return mutableListOf()
        return elanToolchains.toFile().listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.absolutePath }
            ?.toMutableList() ?: mutableListOf()
    }

    /**
     * A valid Lean SDK home must contain bin/lean (the compiler binary).
     */
    override fun isValidSdkHome(path: String): Boolean {
        val home = Path.of(path)
        return home.resolve(LEAN_BIN).exists() || home.resolve("$LEAN_BIN.exe").exists()
    }

    /**
     * Extract version from the toolchain directory name.
     * e.g. "~/.elan/toolchains/leanprover--lean4---v4.29.0" -> "v4.29.0"
     */
    override fun getVersionString(sdkHome: String): String? {
        val dirName = Path.of(sdkHome).fileName?.toString() ?: return null
        // elan dir format: "leanprover--lean4---v4.29.0" -> restore to "leanprover/lean4:v4.29.0"
        val toolchain = dirName.replace("---", ":").replace("--", "/")
        // Extract version part after the last ':'
        val version = toolchain.substringAfterLast(":")
        return version.ifBlank { dirName }
    }

    override fun suggestSdkName(currentSdkName: String?, sdkHome: String): String {
        val version = getVersionString(sdkHome) ?: return "Lean4"
        return "lean4 $version"
    }

    /**
     * Automatically set up source roots when an SDK is configured.
     */
    override fun setupSdkPaths(sdk: Sdk) {
        val homePath = sdk.homePath ?: return
        val srcLean = Path.of(homePath, SRC_LEAN)
        if (!srcLean.isDirectory()) return
        val modificator = sdk.sdkModificator
        // Only add if not already present
        val existingRoots = modificator.getRoots(OrderRootType.CLASSES).map { it.path }
        val srcLeanFile = VfsUtil.findFile(srcLean, true) ?: return
        if (existingRoots.none { it.startsWith(srcLean.toString()) }) {
            modificator.addRoot(srcLeanFile, OrderRootType.CLASSES)
        }
        modificator.commitChanges()
    }

    override fun createAdditionalDataConfigurable(
        sdkModel: SdkModel,
        sdkModificator: SdkModificator
    ): AdditionalDataConfigurable? {
        return null
    }

    override fun getPresentableName(): String {
        return "Lean4"
    }

    override fun getIcon() = Lean4Icons.FILE
}