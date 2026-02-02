package io.github.kmupla.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Locale

/**
 * Entry point for Gradle Plugin.
 */
class ResourceGeneratorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("resourceGenerator", ResourceGeneratorExtension::class.java)

        project.afterEvaluate {
            val textConfigs = extension.getTextResourcesConfigs()
            val generateRTasks = textConfigs.map { config ->
                registerRTask(config, project)
            }

            val assetsConfigs = extension.getBinaryAssetsConfigs()
            val generateAssetsTask = if (assetsConfigs.isNotEmpty()) {
                registerBinaryTask(assetsConfigs, project)
            } else {
                null
            }

            addGeneratedToProjectSourceSets(project, textConfigs, assetsConfigs, generateRTasks, generateAssetsTask)
            setupBuildPhasesDependencies(project, generateRTasks, generateAssetsTask)
        }
    }

    private fun registerBinaryTask(assetsConfigs: List<TaskConfig>, project: Project): TaskProvider<GenerateBytesAssetsTask> {
        return project.tasks.register("generateBytesAssets", GenerateBytesAssetsTask::class.java) {
            val configs = assetsConfigs.map { config ->
                val sourceRoot = config.sourceRoot.get()
                val resourcesDirPath = config.resourcesDir.get()
                val resourcesDir = project.layout.projectDirectory.dir("src/$sourceRoot/$resourcesDirPath").asFile
                val resultName = config.resultObjectName.get()

                // Use unique output dir for each configuration
                val outputDir = project.layout.buildDirectory.dir("generated/assets/kotlin/${resultName.lowercase()}").get().asFile

                BinaryAssetsConfig(
                    resourcesDir = resourcesDir,
                    outputDir = outputDir,
                    packageName = config.packageName.get(),
                    resultObjectName = resultName,
                    includedExtensions = config.includedExtensions.getOrElse(emptySet()),
                    compress = config.compress.get()
                )
            }

            this.configs.set(configs)

            group = "build"
            description = "Generates Kotlin code for all binary assets configurations"
        }
    }

    private fun registerRTask(textConfig: TaskConfig, project: Project): TaskProvider<GenerateRTask> {
        val resultName = textConfig.resultObjectName.get()
        val taskName = "generateRForNative${resultName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"

        return project.tasks.register(taskName, GenerateRTask::class.java) {
            val sourceRoot = textConfig.sourceRoot.get()
            val resourcesDirPath = textConfig.resourcesDir.get()
            resourcesDir.set(project.layout.projectDirectory.dir("src/$sourceRoot/$resourcesDirPath"))

            // Use unique output dir to avoid overlap between tasks
            outputDir.set(project.layout.buildDirectory.dir("generated/resources/kotlin/${resultName.lowercase()}"))

            packageName.set(textConfig.packageName)
            resultObjectName.set(textConfig.resultObjectName)
            if (textConfig.includedExtensions.isPresent) {
                includedExtensions.set(textConfig.includedExtensions)
            }

            // Add to process resources phase
            group = "build"
            description = "Generates Kotlin R file from text resources ($resultName)"
        }
    }

    private fun addGeneratedToProjectSourceSets(
        project: Project,
        textConfigs: List<TaskConfig>,
        assetsConfigs: List<TaskConfig>,
        generateRTasks: List<TaskProvider<GenerateRTask>>,
        generateAssetsTask: TaskProvider<GenerateBytesAssetsTask>?
    ) {
        project.extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { kotlin ->
            textConfigs.zip(generateRTasks).forEach { (config, task) ->
                val sourceRoot = config.sourceRoot.get()
                kotlin.sourceSets.getByName(sourceRoot).kotlin.srcDir(
                    task.flatMap { it.outputDir }
                )
            }

            if (generateAssetsTask != null) {
                // Add all output directories from the single task to their respective source roots
                assetsConfigs.forEach { config ->
                    val sourceRoot = config.sourceRoot.get()
                    val resultName = config.resultObjectName.get()
                    val outputDir = project.layout.buildDirectory.dir("generated/assets/kotlin/${resultName.lowercase()}")

                    kotlin.sourceSets.getByName(sourceRoot).kotlin.srcDir(outputDir)
                }
            }
        }
    }

    private fun setupBuildPhasesDependencies(
        project: Project,
        generateRTasks: List<TaskProvider<GenerateRTask>>,
        generateAssetsTask: TaskProvider<GenerateBytesAssetsTask>?
    ) {
        // Make Kotlin compilation depend on generation tasks - this ensures tasks run before compilation
        project.tasks.withType(KotlinCompile::class.java) {
            generateRTasks.forEach { dependsOn(it) }
            if (generateAssetsTask != null) {
                dependsOn(generateAssetsTask)
            }
        }

        // Also make processResources depend on generation tasks (if it exists)
        project.tasks.findByName("processResources")?.let { processResources ->
            generateRTasks.forEach { processResources.dependsOn(it) }
            if (generateAssetsTask != null) {
                processResources.dependsOn(generateAssetsTask)
            }
        }

        // Make the classes task depend on generation tasks to ensure they run during build
        project.tasks.findByName("classes")?.let { classes ->
            generateRTasks.forEach { classes.dependsOn(it) }
            if (generateAssetsTask != null) {
                classes.dependsOn(generateAssetsTask)
            }
        }
    }

}