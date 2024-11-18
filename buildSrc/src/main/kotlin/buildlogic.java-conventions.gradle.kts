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
    maven("https://oss.sonatype.org/content/repositories/snapshots/") // Snapshots
    maven("https://repo.viaversion.com/") // Viaversion
    maven("https://repo.pgm.fyi/snapshots") // Sportpaper & other pgm-specific stuff
    maven("https://repo.dmulloy2.net/repository/public/") // ProtocolLib repo
    maven("https://repo.papermc.io/repository/maven-public/") // Paper builds & paperweight plugin
}

dependencies {
    api("org.jdom:jdom2:2.0.6.1")
    api("net.kyori:adventure-api:4.17.0")
    api("net.kyori:adventure-text-serializer-plain:4.17.0")
    api("net.kyori:adventure-platform-bukkit:4.3.4")
    api("org.incendo:cloud-core:2.0.0")
    api("org.incendo:cloud-annotations:2.0.0")
    api("org.incendo:cloud-paper:2.0.0-beta.10")
    api("org.incendo:cloud-minecraft-extras:2.0.0-beta.10")
    api("me.lucko:commodore:2.2")
    api("fr.mrmicky:fastboard:2.1.3")
    api("fr.minuskube.inv:smart-invs:1.2.7") { exclude("*") }
    api("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r") { exclude("*") }
    api("net.objecthunter:exp4j:0.4.9-pgm")
    api("org.reflections:reflections:0.10.2")

    // Optional plugin deps
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")
    compileOnly("com.viaversion:viaversion-api:5.0.0")

    // Minecraft includes these (or equivalents)
    compileOnly("it.unimi.dsi:fastutil:8.1.0")
    compileOnly("com.google.guava:guava:17.0")
    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly("commons-lang:commons-lang:2.6")
}

group = "tc.oc.pgm"
version = "0.16-SNAPSHOT"
description = "The original PvP Game Manager for Minecraft"

tasks {
    withType<JavaCompile>() {
        options.encoding = "UTF-8"
    }
    withType<Javadoc>() {
        options.encoding = "UTF-8"
    }
}

spotless {
    ratchetFrom = "origin/dev"
    java {
        removeUnusedImports()
        palantirJavaFormat("2.47.0").style("GOOGLE").formatJavadoc(true)
    }
}

restrictImports {
    group {
        reason = "Use org.jetbrains.annotations to add annotations"
        bannedImports = listOf("javax.annotation.**")
    }
    group {
        reason = "Use tc.oc.pgm.util.Assert to add assertions"
        bannedImports = listOf("com.google.common.base.Preconditions.**", "java.util.Objects.requireNonNull")
    }
}