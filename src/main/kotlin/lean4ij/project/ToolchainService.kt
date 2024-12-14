package lean4ij.project

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class ToolchainService(val project: Project) {
    var toolChainPath: Path? = null
    var lakePath:  Path? = null
    var leanPath: Path? = null
}