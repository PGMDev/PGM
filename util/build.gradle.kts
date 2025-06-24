plugins {
    id("buildlogic.java-conventions")
    `maven-publish`
}

dependencies {
    compileOnly("dev.pgm.paper:paper-api:1.8_1.21.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

sourceSets {
    main {
        resources {
            srcDirs(
                "src/main/resources",
                "src/main/i18n/templates",
                "src/main/i18n/translations")
        }
    }
}

publishing {
    publications.create<MavenPublication>("pgm") {
        groupId = project.group as String
        artifactId = project.name
        version = project.version as String

        artifact(tasks["jar"])
    }
    repositories {
        maven {
            name = "ghPackages"
            url = uri("https://maven.pkg.github.com/PGMDev/PGM")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
