import org.gradle.api.tasks.JavaExec

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

application {
    mainClass.set("com.fmps.autotrader.desktop.DesktopAppKt")
    applicationDefaultJvmArgs = listOf("--add-opens=javafx.graphics/javafx.stage=ALL-UNNAMED")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web", "javafx.swing")
}

val ktorVersion = "2.3.7"
val koinVersion = "3.5.0"

dependencies {
    // Shared module
    implementation(project(":shared"))
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.7.3")
    
    // Ktor Client (to communicate with core-service)
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    
    // TornadoFX (JavaFX framework for Kotlin)
    implementation("no.tornado:tornadofx:1.7.20")

    // Dependency Injection
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    
    // Charts
    implementation("org.controlsfx:controlsfx:11.2.0")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("org.testfx:testfx-core:4.0.17")
    testImplementation("org.testfx:testfx-junit5:4.0.17")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
}

tasks.register<JavaExec>("runDesktopWindows") {
    group = "application"
    description = "Run the Desktop UI with Windows-specific rendering optimisations"
    mainClass.set(application.mainClass)
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs(
        "--add-opens=javafx.graphics/javafx.stage=ALL-UNNAMED",
        "-Dprism.order=d3d",
        "-Dglass.platform=win"
    )
    standardInput = System.`in`
}

tasks.register<JavaExec>("runDesktopMac") {
    group = "application"
    description = "Run the Desktop UI with macOS-specific rendering optimisations"
    mainClass.set(application.mainClass)
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs(
        "--add-opens=javafx.graphics/javafx.stage=ALL-UNNAMED",
        "-Dprism.order=es2",
        "-Dglass.platform=mac"
    )
    standardInput = System.`in`
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

