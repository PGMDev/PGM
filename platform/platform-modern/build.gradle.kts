plugins {
    id("buildlogic.java-conventions")
    id("io.papermc.paperweight.userdev") version "1.7.4"
}

dependencies {
    implementation(project(":util"))
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
}
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
