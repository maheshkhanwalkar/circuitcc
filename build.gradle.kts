plugins {
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "3.0.0"
}

group = "edu.columbia"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// For creating a "fat" JAR containing all the dependencies
application {
    mainClass.set("edu.columbia.circuitc.MainKt")
}
ktor {
    fatJar {
        archiveFileName.set("circuitcc.jar")
    }
}

dependencies {
    // Jackson - JSON parser
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.18.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
