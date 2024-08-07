package com.github.onriv.ijpluginlean.util

import java.net.InetAddress
import java.net.ServerSocket
import java.util.*
import javax.net.ServerSocketFactory


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

    fun findAvailableTcpPort(): Int {
        val minPort = 50000
        val maxPort = 65500
        val portRange = maxPort - minPort
        val maxAttempts = 1000
        var candidatePort: Int
        var searchCounter = 0
        val random: Random = Random(System.nanoTime())
        do {
            check(searchCounter <= maxAttempts) {
                String.format(
                    "Could not find an available TCP port in the range [%d, %d] after %d attempts",
                    minPort, maxPort, maxAttempts
                )
            }
            candidatePort = minPort + random.nextInt(portRange + 1)
            searchCounter++
        } while (!isPortAvailable(candidatePort))

        return candidatePort
    }

    private fun isPortAvailable(port: Int): Boolean {
        try {
            val serverSocket: ServerSocket =
                ServerSocketFactory.getDefault().createServerSocket(port, 1, InetAddress.getByName("localhost"))
            serverSocket.close()
            return true
        } catch (ex: Exception) {
            return false
        }
    }

}