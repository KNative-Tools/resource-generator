plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.rss.knative.tools.plugins"
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
//        create<PluginDeclaration<*>>("resourceGenerator") {
//            id = "io.rss.knative.tools.resource-generator"
//            implementationClass = "io.rss.knative.tools.plugins.ResourceGeneratorPlugin"
//            displayName = "K/N Resource Generator Plugin"
//            description = "Generates a Kotlin R file from resource files in order to embed them into the native application"
//        }

        register("resourceGenerator") {
            id = "io.rss.knative.tools.resource-generator"
            implementationClass = "io.rss.knative.tools.plugins.ResourceGeneratorPlugin"
            displayName = "K/N Resource Generator Plugin"
            description = "Generates a Kotlin R file from resource files in order to embed them into the native application"
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