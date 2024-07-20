package com.github.onriv.ijpluginlean.util

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object Lean4Util {
    fun runCommand(command: String, workingDir: File): String? {
        try {
            val parts = command.split("\\s".toRegex())
            val process = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            process.waitFor(60, TimeUnit.MINUTES)
            return process.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun runCommand(command: String): String? {
        try {
            val process = Runtime.getRuntime().exec(command)
            process.waitFor(60, TimeUnit.MINUTES)
            return process.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

}