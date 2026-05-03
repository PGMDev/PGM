import de.skuzzle.restrictimports.gradle.RestrictImports

plugins {
    `java-library`
    id("com.diffplug.spotless")
    id("de.skuzzle.restrictimports")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // Paper builds & paperweight plugin
    maven("https://oss.sonatype.org/content/repositories/snapshots/") // Snapshots
    maven("https://repo.viaversion.com/") // ViaVersion
    maven("https://repo.pgm.fyi/snapshots") // SportPaper & other PGM-specific stuff
    maven("https://repo.codemc.io/repository/maven-releases/") // PacketEvents
    exclusiveContent {
        forRepository {
            maven("https://jitpack.io")
        }
        filter {
            includeGroup("com.github.OvercastCommunity.adventure-platform")
            includeGroup("com.github.MinusKube")
        }
    }
}

dependencies {
    api("org.jdom:jdom2:2.0.6.1")
    api("net.kyori:adventure-api:5.2.0")
    api("net.kyori:adventure-text-serializer-plain:5.2.0")
    // adventure-platform fork supporting Adventure 5.x
    // https://github.com/OvercastCommunity/adventure-platform
    api("com.github.OvercastCommunity.adventure-platform:adventure-platform-bukkit:b4bfb8a6b4")
    api("org.incendo:cloud-core:2.1.0")
    api("org.incendo:cloud-annotations:2.1.0")
    api("org.incendo:cloud-paper:2.0.0")
    api("org.incendo:cloud-minecraft-extras:2.0.0")
    api("me.lucko:commodore:2.2")
    api("fr.mrmicky:fastboard:2.2.0")
    // Latest SmartInvs commit
    api("com.github.MinusKube:SmartInvs:9c9dbbe") { isTransitive = false }
    api("org.eclipse.jgit:org.eclipse.jgit:7.7.1.202607240634-r") { isTransitive = false }
    api("net.objecthunter:exp4j:0.4.9-pgm")
    api("org.reflections:reflections:0.10.2")

    // Annotations
    api("org.jspecify:jspecify:1.0.1")
    compileOnly("org.jetbrains:annotations:26.1.0")

    // Optional runtime dependencies
    compileOnly("com.github.retrooper:packetevents-spigot:2.13.0")
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
        palantirJavaFormat("2.96.0").style("GOOGLE").formatJavadoc(true)
    }
}

tasks {
    // Bypass inability to have two groups with same base packages inside the import restriction plugin
    val restrictJavaxAnnotations = register<RestrictImports>("restrictJavaxAnnotations") {
        group = "verification"
        reason = "Use org.jspecify.annotations to add annotations, or org.jetbrains.annotations if needed"
        bannedImports = listOf("javax.annotation.**")
    }
    val restrictAsserts = register<RestrictImports>("restrictAsserts") {
        group = "verification"
        reason = "Use tc.oc.pgm.util.Assert to add assertions"
        bannedImports = listOf("com.google.common.base.Preconditions.**", "java.util.Objects.requireNonNull")
    }
    // Enforce the import restrictions
    check {
        dependsOn(restrictJavaxAnnotations, restrictAsserts)
    }
}