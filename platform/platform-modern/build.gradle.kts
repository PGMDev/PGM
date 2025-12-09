plugins {
    id("buildlogic.java-conventions")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":util"))
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
}
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
