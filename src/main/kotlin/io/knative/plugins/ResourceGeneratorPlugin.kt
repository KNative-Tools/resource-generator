package io.knative.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Entry point for Gradle Plugin.
 */
class ResourceGeneratorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("resourceGenerator", ResourceGeneratorExtension::class.java)

        project.afterEvaluate {
            val textConfig = extension.getTextResourcesConfig()
            val generateRTask = registerRTask(textConfig, project)

            val assetsConfig = extension.getBinaryAssetsConfig()
            val generateAssetsTask = registerBinaryTask(assetsConfig, project)

            addGeneratedToProjectSourceSets(project, textConfig, assetsConfig, generateRTask, generateAssetsTask)
            setupBuildPhasesDependencies(project, generateRTask, generateAssetsTask)
        }
    }

    private fun registerBinaryTask(assetsConfig: TaskConfig?, project: Project): TaskProvider<GenerateBytesAssetsTask>? =
        if (assetsConfig != null) {
            project.tasks.register("generateBytesAssets", GenerateBytesAssetsTask::class.java) {
                val sourceRoot = assetsConfig.sourceRoot.get()
                val resourcesDirPath = assetsConfig.resourcesDir.get()
                resourcesDir.set(project.layout.projectDirectory.dir("src/$sourceRoot/$resourcesDirPath"))
                outputDir.set(project.layout.buildDirectory.dir("generated/assets/kotlin"))
                packageName.set(assetsConfig.packageName)
                resultObjectName.set(assetsConfig.resultObjectName)

                // Add to process resources phase
                group = "build"
                description = "Generates Kotlin code for binary assets"
            }
        } else null

    private fun registerRTask(textConfig: TaskConfig?, project: Project): TaskProvider<GenerateRTask>? =
        if (textConfig != null) {
            project.tasks.register("generateRForNative", GenerateRTask::class.java) {
                val sourceRoot = textConfig.sourceRoot.get()
                val resourcesDirPath = textConfig.resourcesDir.get()
                resourcesDir.set(project.layout.projectDirectory.dir("src/$sourceRoot/$resourcesDirPath"))
                outputDir.set(project.layout.buildDirectory.dir("generated/resources/kotlin"))
                packageName.set(textConfig.packageName)
                resultObjectName.set(textConfig.resultObjectName)

                // Add to process resources phase
                group = "build"
                description = "Generates Kotlin R file from text resources"
            }
        } else null

    private fun addGeneratedToProjectSourceSets(
        project: Project,
        textConfig: TaskConfig?,
        assetsConfig: TaskConfig?,
        generateRTask: TaskProvider<GenerateRTask>?,
        generateAssetsTask: TaskProvider<GenerateBytesAssetsTask>?
    ) {
        project.extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { kotlin ->
            textConfig?.let {
                val sourceRoot = it.sourceRoot.get()
                if (generateRTask != null) {
                    kotlin.sourceSets.getByName(sourceRoot).kotlin.srcDir(
                        generateRTask.flatMap { task -> task.outputDir }
                    )
                }
            }
            assetsConfig?.let {
                val sourceRoot = it.sourceRoot.get()
                if (generateAssetsTask != null) {
                    kotlin.sourceSets.getByName(sourceRoot).kotlin.srcDir(
                        generateAssetsTask.flatMap { task -> task.outputDir }
                    )
                }
            }
        }
    }

    private fun setupBuildPhasesDependencies(
        project: Project,
        generateRTask: TaskProvider<GenerateRTask>?,
        generateAssetsTask: TaskProvider<GenerateBytesAssetsTask>?
    ) {
        // Make Kotlin compilation depend on generation tasks - this ensures tasks run before compilation
        project.tasks.withType(KotlinCompile::class.java) {
            generateRTask?.let { dependsOn(it) }
            generateAssetsTask?.let { dependsOn(it) }
        }

        // Also make processResources depend on generation tasks (if it exists)
        project.tasks.findByName("processResources")?.let { processResources ->
            generateRTask?.let { processResources.dependsOn(it) }
            generateAssetsTask?.let { processResources.dependsOn(it) }
        }

        // Make the classes task depend on generation tasks to ensure they run during build
        project.tasks.findByName("classes")?.let { classes ->
            generateRTask?.let { classes.dependsOn(it) }
            generateAssetsTask?.let { classes.dependsOn(it) }
        }
    }

}