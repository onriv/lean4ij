import com.github.gradle.node.npm.task.NpmTask
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    // from https://github.com/ktorio/ktor-samples/blob/main/sse/build.gradle.kts
    id("io.ktor.plugin") version "2.3.12"
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin

    // this is for using nodejs in gradle
    // TODO maybe later we can do kotlin-multiplatform
    //      see: https://stackoverflow.com/questions/78493876/kotlin-gradle-multiplatform-not-producing-nodejs-artifact
    id("com.github.node-gradle.node") version "7.0.2"

}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    // from https://github.com/ktorio/ktor-samples/blob/main/sse/build.gradle.kts
    // check https://mbonnin.medium.com/the-different-kotlin-stdlibs-explained-83d7c6bf293
    // and https://intellij-support.jetbrains.com/hc/en-us/community/posts/360009759780-Error-while-launching-a-coroutine
    // for the exclusion
    // for intellij idea plugin it should not include coroutines
    // TODO this must be -jvm for exclude works...
    //      is it because that only children but not all descendant
    implementation("io.ktor:ktor-server-core-jvm") {
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")
    }
    implementation("io.ktor:ktor-server-netty-jvm") {
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")
    }
    implementation("io.ktor:ktor-server-websockets-jvm") {
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")
    }
    // implementation()
    // for the version, see:
    // https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#coroutinesLibraries
    // this is only for debug
    // uncomment the following for debug coroutines
    /*implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.7.3") {
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")
    }*/
}

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
// this is copied from https://github.com/redhat-developer/intellij-quarkus/blob/main/build.gradle.kts
intellij {
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")
    updateSinceUntilBuild = false

    val platformPlugins =  ArrayList<Any>()
    val localLsp4ij = file("../lsp4ij/build/idea-sandbox/plugins/LSP4IJ").absoluteFile
    if (localLsp4ij.isDirectory) {
        // In case Gradle fails to build because it can't find some missing jar, try deleting
        // ~/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.idea/unzipped.com.jetbrains.plugins/com.redhat.devtools.lsp4ij*
        platformPlugins.add(localLsp4ij)
    } else {
        // When running on CI or when there's no local lsp4ij
        val latestLsp4ijNightlyVersion = fetchLatestLsp4ijNightlyVersion()
        platformPlugins.add("com.redhat.devtools.lsp4ij:$latestLsp4ijNightlyVersion@nightly")
    }
    //Uses `platformPlugins` property from the gradle.properties file.
    // from https://github.com/mallowigi/permify-jetbrains/blob/de27f901228919ce7eab0c37d8045443283fc4eb/build.gradle.kts
    platformPlugins.add("org.jetbrains.plugins.textmate")
    platformPlugins.addAll(properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }.get())
    println("platformPlugins: $platformPlugins")
    plugins = platformPlugins
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token = environment("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = properties("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    val tempPackage = "browser-infoview/node_modules/@leanprover/"
    val targetPath = tempPackage + "infoview"
    val tempPath = tempPackage + "infoview-temp"
    val tempFixCopyToTemp = "tempFixCopyToTemp"
    register<Copy>(tempFixCopyToTemp) {
        dependsOn(npmInstall)
        from(targetPath) {
            include("package.json")
        }
        into(tempPath)
        filter { line ->
            if (line.contains(""""files":""")) {
                // TODO this will duplicated if rerun
                //      TODO check why need this
                """
                    "main": "dist/index",
                    "types": "dist/index",
                    $line
                """.trimIndent()
            } else {
                line.replace("./dist/index.development.js", "./dist/index.production.min.js")
            }
        }
    }

    val tempFixCopyBack = "tempFixCopyBack"
    register<Copy>(tempFixCopyBack) {
        dependsOn(tempFixCopyToTemp)
        from(tempPath) {
            include("package.json")
        }
        into(targetPath)
    }

    val tempFixDelete = "tempFixDelete"
    register<Delete>(tempFixDelete) {
        dependsOn(tempFixCopyBack)
        delete(tempPath)
    }

    register<NpmTask>("npmPackageInstall") {
        dependsOn(npmInstall)
        args.set(listOf("install"))
    }

    register<NpmTask>("buildBrowserInfoview") {
        dependsOn("npmPackageInstall")
        dependsOn(tempFixDelete)
        args.set(listOf("run", "build"))
    }

    buildPlugin {
        dependsOn("buildBrowserInfoview")
    }
}

fun fetchLatestLsp4ijNightlyVersion(): String {
    val client = HttpClient.newBuilder().build();
    var onlineVersion = ""
    try {
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI("https://plugins.jetbrains.com/api/plugins/23257/updates?channel=nightly&size=1"))
            .GET()
            .timeout(Duration.of(10, ChronoUnit.SECONDS))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString());
        val pattern = Pattern.compile("\"version\":\"([^\"]+)\"")
        val matcher = pattern.matcher(response.body())
        if (matcher.find()) {
            onlineVersion = matcher.group(1)
            println("Latest approved nightly build: $onlineVersion")
        }
    } catch (e:Exception) {
        println("Failed to fetch LSP4IJ nightly build version: ${e.message}")
    }

    val minVersion = "0.0.1-20231213-012910"
    return if (minVersion < onlineVersion) onlineVersion else minVersion
}

val processResources by tasks.existing(ProcessResources::class)
processResources {
    from("$rootDir/browser-infoview/dist")
}

node {
    // Whether to download and install a specific Node.js version or not
    // If false, it will use the globally installed Node.js
    // If true, it will download node using above parameters
    // Note that npm is bundled with Node.js
    download = true

    // Version of node to download and install (only used if download is true)
    // It will be unpacked in the workDir
    version = "18.17.1"

    // Version of npm to use
    // If specified, installs it in the npmWorkDir
    // If empty, the plugin will use the npm command bundled with Node.js
    npmVersion = ""

    // Version of Yarn to use
    // Any Yarn task first installs Yarn in the yarnWorkDir
    // It uses the specified version if defined and the latest version otherwise (by default)
    yarnVersion = ""

    // Base URL for fetching node distributions
    // Only used if download is true
    // Change it if you want to use a mirror
    // Or set to null if you want to add the repository on your own.
    distBaseUrl = "https://nodejs.org/dist"

    // Specifies whether it is acceptable to communicate with the Node.js repository over an insecure HTTP connection.
    // Only used if download is true
    // Change it to true if you use a mirror that uses HTTP rather than HTTPS
    // Or set to null if you want to use Gradle's default behaviour.
    allowInsecureProtocol = null

    // The npm command executed by the npmInstall task
    // By default it is install but it can be changed to ci
    npmInstallCommand = "install"

    // The directory where Node.js is unpacked (when download is true)
    workDir = file("${project.projectDir}/.gradle/nodejs")

    // The directory where npm is installed (when a specific version is defined)
    npmWorkDir = file("${project.projectDir}/.gradle/npm")

    // The directory where yarn is installed (when a Yarn task is used)
    yarnWorkDir = file("${project.projectDir}/.gradle/yarn")

    // The Node.js project directory location
    // This is where the package.json file and node_modules directory are located
    // By default it is at the root of the current project
    nodeProjectDir = file("${project.projectDir}/browser-infoview")
}

