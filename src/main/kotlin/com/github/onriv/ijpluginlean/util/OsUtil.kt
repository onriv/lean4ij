package com.github.onriv.ijpluginlean.util

object OsUtil {

    private fun detectOperatingSystem(): String {
        val osName = System.getProperty("os.name").lowercase()

        return when {
            "windows" in osName -> "Windows"
            listOf("mac", "nix", "sunos", "solaris", "bsd").any { it in osName } -> "*nix"
            else -> "Other"
        }
    }

    fun isWindows() : Boolean {
        return detectOperatingSystem() == "Windows";
    }


}