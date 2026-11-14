plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
}

group = "ru.echosystems.omni"
version = "1.0.0"

application {
    mainClass = "ru.echosystems.omni.ApplicationKt"
}

dependencies {
    api(project(":core"))

    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)

    implementation("org.postgresql:postgresql:42.7.8")
    implementation("com.zaxxer:HikariCP:6.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}