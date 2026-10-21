plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "eu.gaelicgames"
version = "1.0-ALPHA"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:0.15.0")
    implementation("io.ktor:ktor-client-cio:3.5.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.5.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.slf4j:slf4j-simple:2.0.18")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:3.5.1")
}

application {
    mainClass.set("eu.gaelicgames.referee.mcp.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("mcp-server")
    archiveClassifier.set("all")
    archiveVersion.set("")
}
