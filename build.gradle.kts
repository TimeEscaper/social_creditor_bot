import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.20"
    application
}

group = "ru.timeescaper"
version = "0.1.0-SNAPSHOT"
val telegrambotapiVersion = "0.35.0"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("dev.inmo:tgbotapi.core:$telegrambotapiVersion")
    implementation("dev.inmo:tgbotapi.extensions.api:$telegrambotapiVersion")
    implementation("dev.inmo:tgbotapi.extensions.utils:$telegrambotapiVersion")
    implementation("dev.inmo:tgbotapi.extensions.behaviour_builder:$telegrambotapiVersion")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "ServerKt"
}