import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("buildlogic.java-conventions")
    `maven-publish`
    id("com.gradleup.shadow")
}

dependencies {
    compileOnly("dev.pgm.paper:paper-api:1.8_1.21.1-SNAPSHOT")

    implementation(project(":util"))
    runtimeOnly(project(":platform-sportpaper")) { exclude("*") }
    runtimeOnly(project(":platform-modern")) { exclude("*") }
}


tasks.named<ShadowJar>("shadowJar") {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
    archiveFileName = "PGM.jar"
    destinationDirectory = rootProject.projectDir.resolve("build/libs")

    minimize {
        // Exclude from minimization as they're required at runtime
        exclude(project(":platform-sportpaper"))
        exclude(project(":platform-modern"))
    }

    dependencies {
        exclude(dependency("com.mojang:brigadier")) // Added by commodore
        // Several compile-only annotation dependencies
        exclude(dependency("org.jetbrains:annotations"))
        exclude(dependency("org.checkerframework:checker-qual"))
        exclude(dependency("org.apiguardian:apiguardian-api"))
    }

    fun pgmRelocate(basePckg: String) = relocate(basePckg, "tc.oc.pgm.lib.$basePckg")

    pgmRelocate("org.incendo.cloud")
    pgmRelocate("io.leangen.geantyref")
    pgmRelocate("me.lucko.commodore")
    pgmRelocate("fr.mrmicky")
    pgmRelocate("org.jdom2")
    pgmRelocate("org.eclipse.jgit")
    pgmRelocate("org.slf4j")

    exclude("META-INF/**")
    exclude("**/*.html")
    exclude("javax/**") // Unsure why this is even added

    // Trim unused parts of javassist
    exclude("javassist/bytecode/analysis/**")
    exclude("javassist/bytecode/stackmap/**")
    exclude("javassist/compiler/**")
}

publishing {
    publications.create<MavenPublication>("pgm") {
        groupId = rootProject.group as String
        artifactId = project.name
        version = rootProject.version as String

        artifact(tasks["shadowJar"])
    }
    repositories {
        maven {
            name = "ghPackages"
            url = uri("https://github.com/PGMDev/PGM")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks {
    processResources {
        filesMatching(listOf("plugin.yml")) {
            expand(
                "name" to project.name,
                "description" to project.description,
                "mainClass" to "tc.oc.pgm.PGMPlugin",
                "version" to project.version,
                "commitHash" to project.latestCommitHash(),
                "url" to "https://pgm.dev/")
        }
    }

    named("build") {
        dependsOn(shadowJar)
    }
}