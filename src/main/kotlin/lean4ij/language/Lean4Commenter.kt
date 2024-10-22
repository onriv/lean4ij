package lean4ij.language

import com.intellij.lang.Commenter

/**
 * TODO don't know if lsp4ij and the lean language server
 *      has this or not
 * ref: maybe https://github.com/intellij-rust/intellij-rust/issues/5171
 * and https://github.com/intellij-rust/intellij-rust/blob/master/src/main/kotlin/org/rust/ide/commenter/RsCommenter.kt
 * and maybe
 * https://github.com/Nordgedanken/intellij-autohotkey/blob/main/src/main/kotlin/com/autohotkey/ide/commenter/AhkCommenter.kt
 * TODO as the following note, it seems some subtly here. And I am not sure what
 */
class Lean4Commenter : Commenter {
    override fun getLineCommentPrefix(): String {
        return "-- "
    }

    override fun getBlockCommentPrefix(): String {
        return "/-- "
    }

    override fun getBlockCommentSuffix(): String {
        return "-/"
    }

    override fun getCommentedBlockCommentPrefix(): String {
        return "/--"
    }

    override fun getCommentedBlockCommentSuffix(): String {
        return "-/"
    }
}