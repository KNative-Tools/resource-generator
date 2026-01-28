package io.knative.plugins

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

abstract class TaskConfig @Inject constructor(objects: ObjectFactory) {
    val packageName: Property<String> = objects.property(String::class.java)
    val resultObjectName: Property<String> = objects.property(String::class.java)
    val sourceRoot: Property<String> = objects.property(String::class.java)
    val resourcesDir: Property<String> = objects.property(String::class.java)
    val includedExtensions: SetProperty<String> = objects.setProperty(String::class.java)
    val compress: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
}

abstract class ResourceGeneratorExtension @Inject constructor(private val objects: ObjectFactory) {
    private val textResourcesConfigs = mutableListOf<TaskConfig>()
    private val binaryAssetsConfigs = mutableListOf<TaskConfig>()

    fun textResources(action: Action<TaskConfig>) {
        val config = objects.newInstance(TaskConfig::class.java).apply {
            packageName.convention("io.knative.resources")
            resultObjectName.convention("R")
            sourceRoot.convention("commonMain")
            resourcesDir.convention("resources")
        }
        action.execute(config)
        textResourcesConfigs.add(config)
    }

    fun binaryAssets(action: Action<TaskConfig>) {
        val config = objects.newInstance(TaskConfig::class.java).apply {
            packageName.convention("io.knative.assets")
            resultObjectName.convention("Assets")
            sourceRoot.convention("commonMain")
            resourcesDir.convention("composeResources/files")
        }
        action.execute(config)
        binaryAssetsConfigs.add(config)
    }

    internal fun getTextResourcesConfigs(): List<TaskConfig> = textResourcesConfigs
    internal fun getBinaryAssetsConfigs(): List<TaskConfig> = binaryAssetsConfigs
}