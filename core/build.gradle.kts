import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    id("buildlogic.java-conventions")
    `maven-publish`
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

val sportPaperServer = configurations.create("sportPaperServer") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    compileOnly("dev.pgm.paper:paper-api:1.8_26.2-SNAPSHOT")
    sportPaperServer("app.ashcon:sportpaper:1.8.8-R0.1-SNAPSHOT")

    implementation(project(":util"))
    runtimeOnly(project(":platform-sportpaper")) { exclude("*") }
    runtimeOnly(project(":platform-modern")) { exclude("*") }
}

val mainOutput = sourceSets.main.get().output
val runtimeClasspath = configurations.getByName("runtimeClasspath")
val configurePgmJar: ShadowJar.(String) -> Unit = { fileName ->
    archiveFileName = fileName
    from(mainOutput)
    configurations = listOf(runtimeClasspath)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependencies {
        exclude(dependency("com.mojang:brigadier")) // Added by commodore
        // Several compile-only annotation dependencies
        exclude(dependency("org.jetbrains:annotations"))
        exclude(dependency("org.checkerframework:checker-qual"))
        exclude(dependency("org.apiguardian:apiguardian-api"))
    }

    exclude("META-INF/**")
    exclude("OSGI-INF/")
    exclude("**/*.html")
    exclude("javax/**") // Unsure why this is even added

    // Trim unused parts of javassist
    exclude("javassist/bytecode/analysis/**")
    exclude("javassist/bytecode/stackmap/**")
    exclude("javassist/compiler/**")
}

tasks.named<ShadowJar>("shadowJar") {
    configurePgmJar("PGM.jar")
    archiveClassifier.set("")
    destinationDirectory = rootProject.projectDir.resolve("build/libs")

    minimize {
        // Exclude from minimization as they're required at runtime
        exclude(project(":platform-sportpaper"))
        exclude(project(":platform-modern"))
    }

    fun pgmRelocate(basePckg: String) = relocate(basePckg, "tc.oc.pgm.lib.$basePckg")

    pgmRelocate("org.incendo.cloud")
    pgmRelocate("io.leangen.geantyref")
    pgmRelocate("me.lucko.commodore")
    pgmRelocate("fr.mrmicky")
    pgmRelocate("org.jdom2")
    pgmRelocate("org.eclipse.jgit")
    pgmRelocate("org.slf4j")
}

val debugJar = tasks.register<ShadowJar>("debugJar") {
    configurePgmJar("PGM-debug.jar")
}

publishing {
    publications.create<MavenPublication>("pgm") {
        groupId = project.group as String
        artifactId = project.name
        version = project.version as String

        artifact(tasks["shadowJar"])
    }
    repositories {
        maven {
            name = "ghPackages"
            url = uri("https://maven.pkg.github.com/PGMDev/PGM")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

val runPaperDirectory = rootProject.layout.projectDirectory.dir(
    providers.gradleProperty("pgm.runPaper.directory").orElse("run/paper"))
val runSportPaperDirectory = rootProject.layout.projectDirectory.dir(
    providers.gradleProperty("pgm.runSportPaper.directory").orElse("run/sportpaper"))

tasks {
    register<RunServer>("runPaper") {
        group = "run paper"
        description = "Run a Paper server for debugging."

        minecraftVersion("26.2")
        systemProperty("terminal.ansi", "true")
        pluginJars(debugJar.flatMap { it.archiveFile })
        downloadPlugins {
            modrinth("packetevents", "2.13.0+spigot")
        }

        runDirectory.set(runPaperDirectory)
    }

    register<RunServer>("runSportPaper") {
        group = "run paper"
        description = "Run a SportPaper server for debugging."

        minecraftVersion("1.8.8")
        legacyPluginLoading()
        systemProperty("terminal.ansi", "true")
        pluginJars(debugJar.flatMap { it.archiveFile })

        runClasspath.from(sportPaperServer)
        runDirectory.set(runSportPaperDirectory)
    }

    processResources {
        val name = project.name
        val description = project.description
        val version = project.version.toString()
        val commitHash = project.latestCommitHash()

        inputs.property("commitHash", commitHash)

        filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
            expand(
                mapOf(
                    "name" to name,
                    "description" to description,
                    "apiVersion" to "26.2",
                    "mainClass" to "tc.oc.pgm.PGMPlugin",
                    "version" to version,
                    "commitHash" to commitHash.get(),
                    "url" to "https://pgm.dev/"
                )
            )
        }
    }

    named("build") {
        dependsOn(shadowJar)
    }
}