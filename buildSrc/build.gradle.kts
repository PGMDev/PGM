plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation("com.gradleup.shadow:com.gradleup.shadow.gradle.plugin:9.3.1")
    implementation("com.diffplug.gradle.spotless:com.diffplug.gradle.spotless.gradle.plugin:8.2.1")
    implementation("de.skuzzle.restrictimports:de.skuzzle.restrictimports.gradle.plugin:3.0.0")
}
