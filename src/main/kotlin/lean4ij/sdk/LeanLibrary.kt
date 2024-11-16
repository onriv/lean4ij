package lean4ij.sdk

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import java.util.function.BooleanSupplier
import javax.swing.Icon

class LeanLibraryExcludeFileCondition : SyntheticLibrary.ExcludeFileCondition {
    companion object {
        // val EXCLUDE_NAMES = arrayOf("test", "deps", "docs")
        val EXCLUDE_NAMES = arrayOf("docs")
    }

    override fun shouldExclude(
        isDir: Boolean,
        filename: String,
        isRoot: BooleanSupplier,
        isStrictRootChild: BooleanSupplier,
        hasParentNotGrandparent: BooleanSupplier
    ): Boolean {
        val result = when {
            isRoot.asBoolean -> false
            filename.startsWith(".") -> true
            filename in EXCLUDE_NAMES -> isDir
            else -> !filename.endsWith(".lean")
        }
        return result
    }

}

class LeanLibrary(
    private val name: String,
    private val root: VirtualFile,
) : SyntheticLibrary(null, LeanLibraryExcludeFileCondition()), ItemPresentation {

    companion object {
        // TODO this absolutely should be check with detail
        val LIBRARY_ICON = IconLoader.getIcon("/icons/libraryFolder.svg", javaClass)
    }

    override fun hashCode() = root.hashCode()

    override fun equals(other: Any?): Boolean = other is LeanLibrary && other.root == root

    override fun getSourceRoots() = listOf(root)

    override fun getLocationString() = ""

    override fun getIcon(p0: Boolean): Icon = LIBRARY_ICON

    override fun getPresentableText() = name
}