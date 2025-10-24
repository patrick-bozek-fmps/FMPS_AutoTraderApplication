plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("com.fmps.autotrader.core.api.ApplicationKt")
}

dependencies {
    // Shared module
    implementation(project(":shared"))
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
    
    // Ktor Server
    val ktorVersion = "2.3.7"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    
    // Ktor Client (for exchange APIs)
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    
    // Database - Exposed ORM
    val exposedVersion = "0.46.0"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    
    // SQLite Driver
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    
    // HikariCP Connection Pool
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // Flyway Migrations
    implementation("org.flywaydb:flyway-core:10.4.1")
    
    // Configuration
    implementation("com.typesafe:config:1.4.3")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // H2 for integration tests
    testImplementation("com.h2database:h2:2.2.224")
}

// Integration tests configuration
sourceSets {
    create("integrationTest") {
        kotlin {
            srcDir("src/integrationTest/kotlin")
        }
        resources {
            srcDir("src/integrationTest/resources")
        }
        compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
        runtimeClasspath += output + compileClasspath
    }
}

val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration tests"
    group = "verification"
    
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    
    shouldRunAfter(tasks.test)
    
    useJUnitPlatform {
        includeTags("integration")
    }
}

tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
    }
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

// Create combined test task
tasks.register("testAll") {
    dependsOn(tasks.test, integrationTest)
    description = "Runs all tests (unit + integration)"
    group = "verification"
}

