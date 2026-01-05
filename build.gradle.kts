plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.knative.plugins"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

gradlePlugin {
    plugins {
        register("resourceGenerator") {
            id = "io.knative.plugins.resource-generator"
            implementationClass = "io.knative.plugins.ResourceGeneratorPlugin"
            displayName = "K/N Resource Generator Plugin"
            description = "Generates Kotlin files from resource files in order to embed them into the native application"
        }
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}