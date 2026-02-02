plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    signing
}

group = "io.github.kmupla.plugins"
version = "0.9.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

gradlePlugin {
    plugins {
        register("resourceGenerator") {
            id = "io.github.kmupla.plugins.resource-generator"
            implementationClass = "io.github.kmupla.plugins.ResourceGeneratorPlugin"
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

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("Kot MultiPlat. Resource Generator")
                description.set("Plugin to generate Kotlin files from resource files.")
                url.set("https://github.com/kmupla/resource-generator")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("ssricardo")
                        name.set("Ricardo SS")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/kmupla/resource-generator.git")
                    url.set("https://github.com/kmupla/resource-generator.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "localStaging"
            url = uri(layout.buildDirectory.dir("repos/bundles"))
        }
    }
}

signing {
    val isPublicationToMavenLocal = gradle.taskGraph.allTasks.any {
        it.name.contains("publishToMavenLocal", ignoreCase = true)
    }
    isRequired = !isPublicationToMavenLocal

    sign(publishing.publications)
}