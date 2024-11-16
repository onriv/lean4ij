package lean4ij.sdk

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.vfs.VfsUtil
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name

/**
 * from julia-intellij, check src/org/ice1000/julia/lang/module/julia-sdks.kt
 */
class Lean4StdLibraryProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<LeanLibrary> {
        val basePath = project.basePath ?: return listOf()
        return Files.list(Path.of(basePath, ".lake", "packages"))
            .filter { it.isDirectory() }
            .map {
                LeanLibrary(it.name, VfsUtil.findFile(it, true)!!)
        }.toList()
    }

}