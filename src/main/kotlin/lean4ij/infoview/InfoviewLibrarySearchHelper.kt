package lean4ij.infoview

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ext.LibrarySearchHelper
import lean4ij.project.LeanProjectService

class InfoviewLibrarySearchHelper : LibrarySearchHelper {
    override fun isLibraryExists(project: Project): Boolean {
        return project.service<LeanProjectService>().isLeanProject()
    }
}