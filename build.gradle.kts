plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "gay.nyaa"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly(fileTree("../PurrCore/build/libs") { include("*.jar") })
    compileOnly(fileTree("../PurrItems/build/libs") { include("*.jar") })
    compileOnly(fileTree("../PurrCollections/build/libs") { include("*.jar") })

    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    testImplementation(fileTree("../PurrCore/build/libs") { include("*.jar") })
}

tasks {
    shadowJar {
        archiveBaseName.set("PurrMinions")
        archiveClassifier.set("")
        archiveVersion.set("1.0.0")

        minimize()
    }

    build {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }
}

kotlin {
    jvmToolchain(21)
}
