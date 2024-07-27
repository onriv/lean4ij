package com.github.onriv.ijpluginlean.lsp

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.lifecycle.LanguageServerLifecycleManager
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider
import java.io.File
import java.util.*

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
        commands = Arrays.asList(
            // TODO configurable path for lake
            "~/.elan/toolchains/leanprover--lean4---v4.8.0-rc2/bin/lake".replaceFirst(
                "~",
                System.getProperty("user.home")
            )
                .replace("/", File.separator),
            "serve", "--", project.basePath
        )
        workingDirectory = project.basePath
    }
}