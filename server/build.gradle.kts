plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    implementation("app.ashcon:sportpaper:1.8.8-R0.1-SNAPSHOT")
    implementation(project(":core"))
    implementation(project(":util"))
    implementation("log4j:log4j:1.2.12")
}