pluginManagement {
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