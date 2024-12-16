package lean4ij.project

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class ToolchainService(val project: Project) {
    var toolChainPath: Path? = null
    var lakePath:  Path? = null
    var leanPath: Path? = null

    /**
     * Run a lean file using lake env, for lean it's ran as the command
     * `lean --run <file>`,
     * but using lake it handles the imports like Mathlib
     * TODO test arguments and working directory
     */
    fun commandLineForRunningLeanFile(filePath: String, arguments: List<String> = listOf()) = GeneralCommandLine(
        lakePath.toString(),
        "env",
        "lean",
        "--run",
        filePath,
        *arguments.toTypedArray()
    ).apply {
        // TODO it seems that running a file with lake requires the project root as the work directory
        this.workDirectory = Path.of(project.basePath!!).toFile()
    }



}