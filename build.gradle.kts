import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.20"
    application
}

group = "ru.timeescaper"
version = "0.1.0-SNAPSHOT"

// TODO: Move to gradle.properties
val telegrambotapiVersion = "0.35.0"
val exposedVersion = "0.31.1"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
    maven { url = uri("https://repo.panda-lang.org/releases") }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("dev.inmo:tgbotapi.core:$telegrambotapiVersion")
    implementation("dev.inmo:tgbotapi.extensions.api:$telegrambotapiVersion")
    implementation("dev.inmo:tgbotapi.extensions.utils:$telegrambotapiVersion")
    implementation("dev.inmo:tgbotapi.extensions.behaviour_builder:$telegrambotapiVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.30.1")
    implementation("net.dzikoysk:exposed-upsert:1.0.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "entrypoint.bot.BotKt"
}

// https://stackoverflow.com/questions/56921833/kotlin-program-error-no-main-manifest-attribute-in-jar-file/61373175#61373175
// https://gist.github.com/domnikl/c19c7385927a7bef7217aa036a71d807
val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "entrypoint.bot.BotKt"
    }

    // To add all of the dependencies otherwise a "NoClassDefFoundError" error
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}