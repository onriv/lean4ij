package lean4ij.language

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

/**
 * TODO this may be possible integrated with the class [lean4ij.project.LeanFile]
 *      but currently we make it separate
 */
class Lean4PsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, Lean4Language.INSTANCE) {

    override fun getFileType(): FileType {
        return Lean4FileType
    }

    override fun toString(): String {
        return "Lean4 Psi File"
    }
}