plugins {
    id("buildlogic.java-conventions")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
}

dependencies {
    implementation(project(":util"))
    paperweight.paperDevBundle("1.21.6-R0.1-SNAPSHOT")
}
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
