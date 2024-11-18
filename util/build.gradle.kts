plugins {
    id("buildlogic.java-conventions")
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
