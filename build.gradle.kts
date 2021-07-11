import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.20"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    application
}

group = "me.paranoidcake"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://jitpack.io")
    maven(url = "https://dl.bintray.com/kordlib/Kord")
}

dependencies {
    testImplementation(kotlin("test-junit5"))

    // Kord Discord Bot API
    implementation("dev.kord:kord-core:0.7.3")

    // Clikt - Command line parsing
    implementation("com.github.ajalt.clikt:clikt:3.0.1")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

sourceSets {
    main {
        resources {
            srcDir("src/main/resources")
        }
    }
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to "MainKt"
            )
        )
    }
}

application {
    mainClassName = "MainKt"
}