package lean4ij.sdk

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.vfs.VfsUtil
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.notExists

/**
 * from julia-intellij, check src/org/ice1000/julia/lang/module/julia-sdks.kt
 */
class Lean4StdLibraryProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<LeanLibrary> {
        val basePath = project.basePath ?: return listOf()
        val packagesPath = Path.of(basePath, ".lake", "packages")
        if (packagesPath.notExists()) {
            return listOf()
        }
        return Files.list(packagesPath)
            .filter { it.isDirectory() }
            .map {
                LeanLibrary(it.name, VfsUtil.findFile(it, true)!!)
        }.toList()
    }

}