plugins {
    kotlin("jvm") version "2.1.0"
    // Shadow removed — manual fatJar used to avoid ASM 65 issue (shadow 8.1.1 can't read Java 21).
    // If you want relocation/minimize, add org.gradle.shadow 8.3.x + re-enable the shadow block below.
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

// Manual fatJar — bundles runtimeClasspath (kotlin stdlib + gson) without shadow ASM.
// No relocation/minimize (add shadow 8.3.x if you need them).
val shadowJar by tasks.registering(Jar::class) {
    archiveBaseName.set("PurrMinions")
    archiveClassifier.set("")
    archiveVersion.set("1.0.0")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
}

tasks {
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
