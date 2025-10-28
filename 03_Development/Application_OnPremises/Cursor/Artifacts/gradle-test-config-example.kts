// Example Gradle Test Configuration
// Add this to your build.gradle.kts files

plugins {
    kotlin("jvm")
    jacoco  // Code coverage
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"  // Code style
    id("io.gitlab.arturbosch.detekt") version "1.23.3"     // Static analysis
    id("org.owasp.dependencycheck") version "8.4.2"       // Security check
}

// Testing configuration
tasks.test {
    useJUnitPlatform()
    
    // Test execution settings
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    
    // Memory settings
    minHeapSize = "512m"
    maxHeapSize = "2048m"
    
    // Test output
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
        showCauses = true
        showStackTraces = true
    }
    
    // Generate test report
    reports {
        html.required.set(true)
        junitXml.required.set(true)
    }
    
    // Fail build on test failure
    ignoreFailures = false
}

// Integration tests
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

// Code coverage with JaCoCo
jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/models/**",        // Exclude data classes
                    "**/config/**",        // Exclude configuration
                    "**/*Application*",    // Exclude application entry points
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()  // 80% minimum coverage
            }
        }
        
        rule {
            element = "CLASS"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.75".toBigDecimal()  // 75% per class minimum
            }
            
            excludes = listOf(
                "*.models.*",
                "*.config.*",
                "*Application*"
            )
        }
        
        rule {
            element = "PACKAGE"
            includes = listOf(
                "com.fmps.autotrader.core.traders.*",
                "com.fmps.autotrader.core.connectors.*"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()  // 90% for critical packages
            }
        }
    }
}

// ktlint configuration
ktlint {
    version.set("1.0.1")
    verbose.set(true)
    android.set(false)
    
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

// Detekt configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/config/detekt.yml")
    
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
    }
}

// OWASP Dependency Check
dependencyCheck {
    format = "HTML"
    failBuildOnCVSS = 7.0f  // Fail on HIGH or CRITICAL vulnerabilities
    suppressionFile = "$projectDir/config/dependency-check-suppressions.xml"
}

// Test dependencies
dependencies {
    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    
    // Mockk for Kotlin
    testImplementation("io.mockk:mockk:1.13.8")
    
    // Kotest assertions
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    
    // Ktor testing
    testImplementation("io.ktor:ktor-server-test-host:2.3.5")
    testImplementation("io.ktor:ktor-client-mock:2.3.5")
    
    // Database testing
    testImplementation("com.h2database:h2:2.2.224")  // In-memory DB for tests
    
    // TestFX for JavaFX testing
    testImplementation("org.testfx:testfx-core:4.0.17")
    testImplementation("org.testfx:testfx-junit5:4.0.17")
    
    // Coroutine testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

// Create source set for integration tests
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

// Make check task depend on coverage verification
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
    dependsOn(tasks.ktlintCheck)
    dependsOn(tasks.detekt)
}

// Task to run all tests
tasks.register("testAll") {
    dependsOn(tasks.test, integrationTest)
    description = "Runs all tests (unit + integration)"
    group = "verification"
}

