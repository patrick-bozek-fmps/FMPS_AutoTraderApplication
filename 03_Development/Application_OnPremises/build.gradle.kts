plugins {
    kotlin("jvm") version "1.9.21" apply false
    kotlin("plugin.serialization") version "1.9.21" apply false
}

group = "com.fmps.autotrader"
version = "1.0.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    
    group = rootProject.group
    version = rootProject.version
    
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()

        val availableProcessors = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        maxParallelForks = when {
            availableProcessors <= 2 -> availableProcessors
            else -> (availableProcessors - 1).coerceAtMost(4)
        }

        jvmArgs("-Xms512m", "-Xmx2048m")
    }
}
