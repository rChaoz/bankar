@file:Suppress("PropertyName")

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val postgresql_version: String by project

plugins {
    kotlin("jvm")
    id("io.ktor.plugin") version "2.3.11"
    id("org.jetbrains.kotlin.plugin.serialization")
}

group = "ro.bankar"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

ktor {
    distributions.main {
        contents {
            into("bin") {
                from("./").include("*.jks")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    // When DB URL is not set, use in-memory H2 database
    if ("DB_URL" !in environment) environment("DB_URL", "jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;")
    // Set default @TestInstance lifecycle to PER_CLASS
    //systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
}

dependencies {
	implementation(project(":common"))
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auto-head-response-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-freemarker-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-cio-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktor_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposed_version")
    implementation("org.postgresql:postgresql:$postgresql_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("net.axay:simplekotlinmail-core:1.4.0")
    implementation("com.github.librepdf:openpdf:1.3.30")
    implementation(libs.firebase.admin)

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlin_version")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktor_version")
    testImplementation("com.h2database:h2:2.2.224")
}