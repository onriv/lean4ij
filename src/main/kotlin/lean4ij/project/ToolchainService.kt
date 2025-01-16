package lean4ij.project

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.nio.file.Path
import kotlin.io.path.exists

@Service(Service.Level.PROJECT)
class ToolchainService(val project: Project) {
    // set to true when the toolchain could not properly be
    // initialized
    var toolChainPath: Path? = null
    var lakePath:  Path? = null
    var leanPath: Path? = null

    companion object {
        private val ARGUMENT_SEPARATOR = Regex("\\s+")
        const val TOOLCHAIN_FILE_NAME = "lean-toolchain"

        fun expectedToolchainPath(project: Project): Path {
            return Path.of(project.basePath!!, TOOLCHAIN_FILE_NAME)
        }
    }

    fun expectedToolchainPath(): Path {
        return expectedToolchainPath(this.project)
    }

    fun toolchainNotFound(): Boolean {
        return !expectedToolchainPath().toFile().isFile
    }

    /**
     * Run a lean file using lake env, for lean it's ran as the command
     * `lean --run <file>`,
     * but using lake it handles the imports like Mathlib
     * TODO test arguments and working directory
     */
    fun commandLineForRunningLeanFile(filePath: String, arguments: String = ""): GeneralCommandLine {
        val command = mutableListOf(lakePath.toString(), "env", "lean", "--run", filePath)
        if (arguments.isNotEmpty()) {
            command.addAll(arguments.split(ARGUMENT_SEPARATOR))
        }
        return GeneralCommandLine(*command.toTypedArray()).apply {
            // TODO it seems that running a file with lake requires the project root as the work directory
            this.workDirectory = Path.of(project.basePath!!).toFile()
        }
    }

    fun commandForRunningLake(arguments: String): GeneralCommandLine {
        val command = mutableListOf(lakePath.toString())
        // TODO what if it's empty?
        if (arguments.isNotEmpty()) {
            command.addAll(arguments.split(ARGUMENT_SEPARATOR))
        }
        return GeneralCommandLine(*command.toTypedArray()).apply {
            this.workDirectory = Path.of(project.basePath!!).toFile()
        }
    }
}