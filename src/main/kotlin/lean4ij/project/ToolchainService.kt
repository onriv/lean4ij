package lean4ij.project

import com.google.gson.Gson
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import lean4ij.util.fromJson
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.nio.file.Path

data class GitHubTag(val name: String)

@Service(Service.Level.PROJECT)
class ElanService {

    val elanHomePath = Path.of(System.getProperty("user.home"), ".elan")
    val elanBinPath = elanHomePath.resolve("bin")
    val elanPath = elanBinPath.resolve("elan")

    fun getDefaultLakePath(): Path {
        val elanBinPath = Path.of(System.getProperty("user.home"), ".elan", "bin")
        return elanBinPath.resolve("lake")
    }
    fun getDefaultElanPath(): Path {
        val elanBinPath = Path.of(System.getProperty("user.home"), ".elan", "bin")
        return elanBinPath.resolve("elan")
    }

    /**
     * All versions are extracted locally from the lean4 repo with the following shell command:
     * ```
     * git --no-pager tag|grep v4|python -c 'import sys; print("".join(sorted(sys.stdin,key=lambda x:tuple(map(int,x.replace("v","").replace("-rc", ".").replace("-m", ".").split("."))), reverse=True)))'
     * ```
     * TODO maybe it can fetch locally or update in the pipeline
     * A curl version of this using the github api is
     * curl https://api.github.com/repos/leanprover/lean4/tags |grep name|awk '{print $2}'
     */
    fun toolchains(includeRemote: Boolean): List<String> {
        return javaClass.classLoader.getResource("toolchains.txt").readText().split("\n")
    }

    /**
     * Fetching all versions from https://api.github.com/repos/leanprover/lean4/tags
     */
    fun toolchainsFromGithub(proxy: Proxy? = null): List<String> {
        return getGitHubTags("leanprover", "lean4", proxy)
    }

    /**
     * The method here is using the GitHub API to fetch the tags of a repository
     * and then return the names of the tags.
     *
     * The method is from standard library and avoids relying on third party libraries
     */
    fun getGitHubTags(owner: String, repo: String, proxy: Proxy?): List<String> {
        val url = URL("https://api.github.com/repos/$owner/$repo/tags")
        val connection = (if (proxy != null) {
            url.openConnection(proxy)
        } else {
            url.openConnection()
        }) as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                connectTimeout = 5000
                readTimeout = 5000
            }

            return when (connection.responseCode) {
                in 200..299 -> {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Gson().fromJson<List<GitHubTag>>(response).map { it.name }
                }
                else -> throw Exception("HTTP Error ${connection.responseCode}: ${connection.responseMessage}")
            }
        } catch (e: Exception) {
            throw e;
        } finally {
            connection.disconnect()
        }
    }


    fun commandForRunningElan(arguments: String, project: Project, environment: Map<String, String>) : GeneralCommandLine {
        val command = mutableListOf(elanPath.toString())
        // TODO what if it's empty?
        if (arguments.isNotEmpty()) {
            // TODO DRY this
            command.addAll(arguments.split(Regex("\\s+")))
        }
        return GeneralCommandLine(*command.toTypedArray()).apply {
            this.workDirectory = Path.of(project.basePath!!).toFile()
            this.environment.putAll(environment)
        }
    }

}

/**
 * TODO this is project level service
 *      there should be a global service similar for system level elan/lake
 */
@Service(Service.Level.PROJECT)
class ToolchainService(val project: Project) {
    // set to true when the toolchain could not properly be
    // initialized
    var toolChainPath: Path? = null
    var lakePath:  Path? = null
    var leanPath: Path? = null

    // This is for creating new project
    var defaultLakePath: Path? = null
    var defaultElanPath: Path? = null

    companion object {
        // TODO DRY this with the above elan service
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

    fun commandForRunningLake(arguments: String, environments: Map<String, String> = mapOf()): GeneralCommandLine {
        val command = mutableListOf(lakePath.toString())
        // TODO what if it's empty?
        if (arguments.isNotEmpty()) {
            command.addAll(arguments.split(ARGUMENT_SEPARATOR))
        }
        return GeneralCommandLine(*command.toTypedArray()).apply {
            this.workDirectory = Path.of(project.basePath!!).toFile()
            this.environment.putAll(environments)
        }
    }
}