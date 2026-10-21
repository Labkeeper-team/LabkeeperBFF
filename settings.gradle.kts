pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.springframework.boot") version "4.1.0"
        id("org.jetbrains.kotlin.jvm") version "2.4.0"
        id("org.jetbrains.kotlin.plugin.spring") version "2.4.0"
    }
}

rootProject.name = "labkeeper-frontend"
