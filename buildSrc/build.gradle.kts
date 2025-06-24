plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.0.0-beta17")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.4")
    implementation("de.skuzzle.restrictimports:restrict-imports-gradle-plugin:2.6.1")
}
