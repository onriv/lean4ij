/**
 * TODO update gradle plugin to 2.x
 *      https://github.com/onriv/lean4ij/issues/125
 */
import com.github.gradle.node.npm.proxy.ProxySettings
import com.github.gradle.node.npm.task.NpmTask
import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.regex.Pattern


// Load properties from the external file
val properties = Properties()
file("local.properties").let { localPropertiesFile ->
    if (localPropertiesFile.exists() && localPropertiesFile.isFile && localPropertiesFile.canRead()) {
        localPropertiesFile.inputStream().use { properties.load(it) }
    }
}

// Set the proxy properties
properties.forEach { key, value ->
    System.setProperty(key as String, value as String)
}

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    // from https://github.com/ktorio/ktor-samples/blob/main/sse/build.gradle.kts
    id("io.ktor.plugin") version "3.1.2"
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin

    // this is for using nodejs in gradle
    // TODO maybe later we can do kotlin-multiplatform
    //      see: https://stackoverflow.com/questions/78493876/kotlin-gradle-multiplatform-not-producing-nodejs-artifact
    id("com.github.node-gradle.node") version "7.1.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"

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
    // ref: https://github.com/hfhbd/kobol/blob/main/intellij-plugin/build.gradle.kts#L30
    // exclusions have been moved to configurations.runtimeClasspath
    // TODO weird, even moved to configurations.runtimeClasspath, here it still requires depending the jvm version
    //      directly
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-websockets-jvm")
    // implementation()
    // for the version, see:
    // https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#coroutinesLibraries
    // this is only for debug
    // uncomment the following for debug coroutines
    /*implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.7.3") {
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")
    }*/

    /**
     * In local unittest got the following error:
     * Caused by: java.lang.ExceptionInInitializerError:
     * Exception java.lang.UnsatisfiedLinkError:
     * Native library (com/sun/jna/linux-x86-64/libjnidispatch.so) not found in resource path
     * Hence add this, not saw before on GitHub action
     */
    testImplementation("net.java.dev.jna:jna:5.17.0")
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
    platformPlugins.addAll(properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }.get())
    // println("Depends on platformPlugins: $platformPlugins")
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

sourceSets {
    main {
        java.srcDirs("src/main/java", "src/gen/java")
        kotlin.srcDirs("src/main/kotlin")
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

    register<NpmTask>("npmPackageInstall") {
        dependsOn(npmInstall)
        args.set(listOf("install"))
    }

    register<NpmTask>("buildBrowserInfoview") {
        dependsOn("npmPackageInstall")
        args.set(listOf("run", "build"))
    }

    register<NpmTask>("runBrowserInfoview") {
        args.set(listOf("run", "dev"))
    }

    buildPlugin {
        dependsOn("buildBrowserInfoview")
    }

    val genLean4Lexer = register<GenerateLexerTask>("genLean4Lexer") {
        description = "Generates lexer"
        group = "build setup"
        sourceFile.set(file("src/main/grammars/Lean4Lexer.flex"))
        targetOutputDir.set(file("src/gen/java/lean4ij/language"))
        purgeOldFiles.set(true)
    }

    val genLean4Parser = register<GenerateParserTask>("genLean4Parser") {
        description = "Generates parser"
        group = "build setup"
        sourceFile.set(file("src/main/grammars/Lean4Parser.bnf"))
        targetRootOutputDir.set(file("src/gen/java/"))
        pathToParser.set("src/gen/java/lean4ij/language/Lean4Parser.java")
        pathToPsiRoot.set("src/gen/java/lean4ij/language/")
        purgeOldFiles.set(true)
    }

    val deleteGen = register<Delete>("deleteGen") {
        delete("src/gen/java")
    }

    val genLiveTemplate = register("genLiveTemplate") {
        val liveTemplateGenerator = LiveTemplateConverter("src/main/resources/liveTemplates/abbreviations.json")
        liveTemplateGenerator.generate()
        liveTemplateGenerator.generate(true, "Lean4Space", "Lean4-space.xml")
        val pairLiveTemplate = LiveTemplateConverter("src/main/resources/liveTemplates/abbreviations_pairs.json")
        pairLiveTemplate.generate(space=false, context = "Lean4Pair", output="Lean4-pair.xml")
    }

    withType<KotlinCompile>().configureEach {
        dependsOn(deleteGen)
        dependsOn(genLean4Parser)
        dependsOn(genLean4Lexer)
        dependsOn(genLiveTemplate)
    }



}

tasks.test {
    // ref: https://intellij-support.jetbrains.com/hc/en-us/community/posts/4407334950290-jarFiles-is-not-set-for-PluginDescriptor
    // for resolving error "jarFiles is not set for PluginDescriptor"
    systemProperty("idea.force.use.core.classloader", "true")
}

// ref: https://github.com/hfhbd/kobol/blob/main/intellij-plugin/build.gradle.kts#L30
configurations.runtimeClasspath {
    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")
}

fun fetchLatestLsp4ijNightlyVersion(): String {
    val client = HttpClient.newBuilder().build()
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
            println("Depend on the latest approved nightly build of LSP4IJ: $onlineVersion")
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

// ref: https://github.com/node-gradle/gradle-node-plugin/blob/main/docs/usage.md
node {
    // Whether to download and install a specific Node.js version or not
    // If false, it will use the globally installed Node.js
    // If true, it will download node using above parameters
    // Note that npm is bundled with Node.js
    download = true

    // Version of node to download and install (only used if download is true)
    // It will be unpacked in the workDir
    version = "18.18.1"

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

    // Whether the plugin automatically should add the proxy configuration to npm and yarn commands
    // according the proxy configuration defined for Gradle
    // Disable this option if you want to configure the proxy for npm or yarn on your own
    // (in the .npmrc file for instance)
    nodeProxySettings = ProxySettings.FORCED
}

/**
 * generate the (static) live templates,
 *
 * convert the abbreviations.json etc. file adapted from vscode to
 * the xml file used in intellij idea's live template system,
 * ref: https://plugins.jetbrains.com/docs/intellij/providing-live-templates.html
 *
 * the input source mainly comes from
 * https://github.com/leanprover/vscode-lean4/blob/master/lean4-unicode-input/src/abbreviations.json
 * and some adjust for inputting more easily in intellij idea.
 */
class LiveTemplateConverter(private val source: String) {

    fun generate(space: Boolean = false, context: String = "Lean4", output : String = "Lean4.xml") {
        println("generating $source with space: $space, context: $context")
        val abbrev: Map<String, String> = fromJson(File(source).readText(StandardCharsets.UTF_8))
        val tplSet = generateContent(abbrev, space, context)
        output(tplSet, output, context)
    }

    private fun generateContent(abbrev: Map<String, String>, space: Boolean = false, context: String = "Lean4"): List<String> {
        val prefix = "\\"
        val tplSet = mutableListOf<String>()

        for ((k0, v0) in abbrev) {
            // since we cannot expand the live template automatically, we do not need to handle this double backslash
            // though sometimes we do want it automatically expanded...
            if (k0 == "\\") continue

            var v = v0
            var d = v

            if (v.contains("\$CURSOR")) {
                v = v.replace("\$CURSOR", "\$END\$")
                d = d.replace("\$CURSOR", "")
            }

            if (space) {
                v += " "
                d += " with space"
            }

            val k = prefix + escapeXml(k0)
            val tpl = """    <template name="$k" value="$v" shortcut="SPACE" description="$d" toReformat="false" toShortenFQNames="true">
        <context>
          <option name="$context" value="true" />
        </context>
      </template>"""
            tplSet.add(tpl)
        }

        return tplSet
    }

    private fun output(tplSet: List<String>, fileName: String = "Lean4.xml", group: String = "Lean4") {
        val fileContent = """<templateSet group="$group">
${tplSet.joinToString("\n")}
</templateSet>
        """

        val filePath = "src/main/resources/liveTemplates/$fileName"
        File(filePath).writeText(fileContent, Charsets.UTF_8)
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            // TODO this should be escape but the original python script does not do it, weird
            //.replace(">", "&gt;")
            .replace("\"", "&quot;")
            // TODO this should be escape but the original python script does not do it, weird
            //.replace("'", "&apos;")
    }
}

inline fun <reified T> fromJson(json: String) : T {
    return Gson().fromJson(json, T::class.java)
}
