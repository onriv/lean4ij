package com.github.onriv.ijpluginlean.lsp

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.lifecycle.LanguageServerLifecycleManager
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit

internal class LeanLanguageServerProvider(val project: Project) : ProcessStreamConnectionProvider() {
    init {
        setServerCommand()
        addLanguageServerLifecycleListener()
    }

    private fun addLanguageServerLifecycleListener() {
        val instance = LanguageServerLifecycleManager.getInstance(project)
        instance.addLanguageServerLifecycleListener(LeanLanguageServerLifecycleListener(project))
    }

    private fun setServerCommand() {
        val lake = "elan which lake".runCommand(File(project.basePath!!)).trim()
        commands = listOf(lake, "serve", "--", project.basePath)
        workingDirectory = project.basePath
    }

    private val tempLogDir = Files.createTempDirectory(Path.of(PathManager.getTempPath()), "lean-lsp").toString()

    override fun getUserEnvironmentVariables(): MutableMap<String, String> {
        thisLogger().info("lean lsp log dir set to $tempLogDir")
        return mutableMapOf("LEAN_SERVER_LOG_DIR" to tempLogDir)
    }

    private fun String.runCommand(workingDir: File): String {
        try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            proc.waitFor(60, TimeUnit.MINUTES)
            return proc.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            throw e
        }
    }

}