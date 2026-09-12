plugins {
    id("buildlogic.java-conventions")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":util"))
    paperweight.paperDevBundle("26.2.build.+")
}