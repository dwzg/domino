// The online game server: deals, checks every move, plays computer seats and
// pushes each player their own view of the table.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

application {
    mainClass.set("de.wtzg.domino.server.MainKt")
}

// The release version (./gradlew -PappVersion=2.1.0), reported by GET /version.
val serverVersion = (project.findProperty("appVersion") as String?) ?: "dev"
val writeVersion by tasks.registering {
    val out = layout.buildDirectory.dir("generated/version")
    inputs.property("version", serverVersion)
    outputs.dir(out)
    doLast {
        out.get().file("domino-server.properties").asFile.apply {
            parentFile.mkdirs()
            writeText("version=$serverVersion\n")
        }
    }
}
sourceSets.main { resources.srcDir(writeVersion) }

dependencies {
    implementation(project(":protocol"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.json)
    implementation(libs.sqlite.jdbc)
    implementation(libs.logback)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.websockets)
}
