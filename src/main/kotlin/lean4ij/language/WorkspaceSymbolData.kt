package lean4ij.language

import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.FakePsiElement
import com.redhat.devtools.lsp4ij.LSPIJUtils
import com.redhat.devtools.lsp4ij.ui.IconMapper
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.SymbolKind
import java.io.File
import java.util.*
import javax.swing.Icon

/**
 * LSP navigation item implementation.
 */
class LeanWorkspaceSymbolData(name: String,
                           private val symbolKind: SymbolKind,
                           private val fileUri: String,
                           private val position: Position?,
                           private val project: Project) : FakePsiElement() {
    val file: VirtualFile? = LSPIJUtils.findResourceFor(fileUri)
    private val presentation: LSPItemPresentation

    @JvmRecord
    private data class LSPItemPresentation(val name: String, val symbolKind: SymbolKind, val locationString: String?) :
        ItemPresentation {

        override fun getPresentableText(): String {
            return name
        }

        override fun getIcon(unused: Boolean): Icon? {
            return IconMapper.getIcon(symbolKind)
        }

        override fun getLocationString(): String? {
            return locationString
        }

    }

    constructor(name: String,
                symbolKind: SymbolKind,
                location: Location,
                project: Project) : this(name, symbolKind, location.uri, location.range.start, project)

    init {
        val locationString: String? = if (file != null) getLocationString(project, file) else fileUri
        this.presentation = LSPItemPresentation(name, symbolKind, locationString)
    }

    fun getSymbolKind(): SymbolKind {
        return symbolKind
    }

    override fun getName(): String {
        return presentation.name
    }

    override fun getPresentation(): ItemPresentation {
        return presentation
    }

    override fun getParent(): PsiElement? {
        val file = file?:return null
        val psiFile = PsiManager.getInstance(project).findFile(file)
        return psiFile
    }

    override fun navigate(requestFocus: Boolean) {
        LSPIJUtils.openInEditor(fileUri, position, requestFocus, false, project)
    }

    override fun canNavigate(): Boolean {
        return true
    }

    override fun canNavigateToSource(): Boolean {
        return true
    }

    companion object {
        /**
         * This code is a copy/paste from https://github.com/JetBrains/intellij-community/blob/22243811e3e8342918b5c064cbb94c7886d8e3ed/plugins/htmltools/src/com/intellij/htmltools/html/HtmlGotoSymbolProvider.java#L46
         * @param project
         * @param file
         * @return
         */
        private fun getLocationString(project: Project, file: VirtualFile): String? {
            return Optional.ofNullable<VirtualFile>(project.guessProjectDir())
                .map<String?> { projectDir: VirtualFile ->
                    VfsUtilCore.getRelativePath(
                        file,
                        projectDir,
                        File.separatorChar
                    )
                }
                .map { path: String? -> "($path)" }
                .orElse(null)
        }
    }
}