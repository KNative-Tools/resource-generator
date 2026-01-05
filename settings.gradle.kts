pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.0"
    }

    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "resource-generator-plugin"