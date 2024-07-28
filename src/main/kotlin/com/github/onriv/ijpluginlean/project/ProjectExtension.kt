package com.github.onriv.ijpluginlean.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.ConcurrentHashMap

private val leanFiles = ConcurrentHashMap<VirtualFile, LeanFile>()

fun Project.file(file: VirtualFile) : LeanFile {
    return leanFiles.computeIfAbsent(file) {LeanFile(this@file, file)}
}