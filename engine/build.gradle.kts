// Pure Kotlin game logic: rules, scoring, AI, coach and table layout.
// No Android dependencies, so it is fast to test and easy to reason about.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    // Game state is @Serializable so a match can be saved and resumed.
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
