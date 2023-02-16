
val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project



plugins {
    application
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "eu.gaelicgames"
version = "0.0.1"
application {
    mainClass.set("eu.gaelicgames.referee.ApplicationKt")

    val isDevelopment: Boolean = true
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {

    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-freemarker-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-resources:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")

    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("org.jetbrains.exposed", "exposed-core", exposed_version)
    implementation("org.jetbrains.exposed", "exposed-dao", exposed_version)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposed_version)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposed_version)

    implementation("com.nimbusds:nimbus-jose-jwt:9.30.1")

    implementation("com.mailjet", "mailjet-client", "5.2.1")

    implementation("com.natpryce:konfig:1.6.10.0")

    implementation("org.xerial:sqlite-jdbc:3.39.2.0")
    implementation("com.h2database:h2:2.1.212")

    implementation("org.flywaydb:flyway-core:9.1.3")

    implementation("at.favre.lib:bcrypt:0.9.0")
    implementation("org.apache.commons","commons-csv","1.9.0")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

tasks.register("buildNPM") {
    doLast {
        exec {
            workingDir = File("./frontend-vite/")
            commandLine = listOf("npm", "run","mybuild")
        }
    }
}

/*
val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}-fat"
    manifest {
        attributes["Implementation-Title"] = "GGE Referee Fat Jar"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "eu.gaelicgames.referee.ApplicationKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        .duplicatesStrategy = DuplicatesStrategy.INCLUDE
    with(tasks.jar.get() as CopySpec)
}*/
/*
tasks.classes {
    dependsOn("buildNPM")
}*/