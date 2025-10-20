package io.rss.knative.tools.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class ResourceGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val generateRForNative = project.tasks.register<GenerateRTask>("generateRForNative") {
            group = "build"
            description = "Generates R.kt from resources"
            resourcesDir.set(
                project.file("src/commonMain/resources")
            )
            outputDir.set(project.file("${project.buildDir}/generated/nativeR"))
        }

        project.extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain") {
                kotlin.srcDir(generateRForNative.flatMap { it.outputDir })
            }
        }

        project.tasks.withType<KotlinCompile> {
            dependsOn(generateRForNative)
        }
    }
}