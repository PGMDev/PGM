plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":util"))
    compileOnly("app.ashcon:sportpaper:1.8.8-R0.1-SNAPSHOT")
}
