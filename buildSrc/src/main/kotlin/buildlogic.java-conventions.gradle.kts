plugins {
    `java-library`
    id("com.diffplug.spotless")
    id("de.skuzzle.restrictimports")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // Paper builds & paperweight plugin
    maven("https://oss.sonatype.org/content/repositories/snapshots/") // Snapshots
    maven("https://repo.viaversion.com/") // Viaversion
    maven("https://repo.pgm.fyi/snapshots") // Sportpaper & other pgm-specific stuff
}

dependencies {
    api("org.jdom:jdom2:2.0.6.1")
    api("net.kyori:adventure-api:4.26.1")
    api("net.kyori:adventure-text-serializer-plain:4.26.1")
    api("net.kyori:adventure-platform-bukkit:4.4.1")
    api("org.incendo:cloud-core:2.0.0")
    api("org.incendo:cloud-annotations:2.0.0")
    api("org.incendo:cloud-paper:2.0.0-beta.14")
    api("org.incendo:cloud-minecraft-extras:2.0.0-beta.14")
    api("me.lucko:commodore:2.2")
    api("fr.mrmicky:fastboard:2.1.5")
    api("fr.minuskube.inv:smart-invs:1.2.7") { isTransitive = false }
    api("org.eclipse.jgit:org.eclipse.jgit:7.5.0.202512021534-r") { isTransitive = false }
    api("net.objecthunter:exp4j:0.4.9-pgm")
    api("org.reflections:reflections:0.10.2")

    // Annotations
    api("org.jspecify:jspecify:1.0.0")
    compileOnly("org.jetbrains:annotations:26.0.2-1")

    // Optional runtime dependencies
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("com.viaversion:viaversion-api:5.0.0")

    // Paper and SportPaper include these (or equivalents)
    compileOnly("it.unimi.dsi:fastutil:8.5.15")
    compileOnly("com.google.guava:guava:17.0")
    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("org.apache.commons:commons-lang3:3.17.0")
}

group = "tc.oc.pgm"
version = "0.16-SNAPSHOT"
description = "The original PvP Game Manager for Minecraft"

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    withType<Javadoc> {
        options.encoding = "UTF-8"
    }
}

spotless {
    ratchetFrom = "origin/dev"
    java {
        removeUnusedImports()
        trimTrailingWhitespace()
        formatAnnotations()
        palantirJavaFormat("2.87.0").style("GOOGLE").formatJavadoc(true)
    }
}

restrictImports {
    group {
        reason = "Use org.jspecify.annotations to add annotations, or org.jetbrains.annotations if needed"
        bannedImports = listOf("javax.annotation.**")
    }
    group {
        reason = "Use tc.oc.pgm.util.Assert to add assertions"
        bannedImports = listOf("com.google.common.base.Preconditions.**", "java.util.Objects.requireNonNull")
    }
}