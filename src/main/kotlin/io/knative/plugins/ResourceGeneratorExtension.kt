package io.knative.plugins

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class TaskConfig @Inject constructor(objects: ObjectFactory) {
    val packageName: Property<String> = objects.property(String::class.java)
    val resultObjectName: Property<String> = objects.property(String::class.java)
    val sourceRoot: Property<String> = objects.property(String::class.java)
    val resourcesDir: Property<String> = objects.property(String::class.java)
}

abstract class ResourceGeneratorExtension @Inject constructor(private val objects: ObjectFactory) {
    private var textResourcesConfig: TaskConfig? = null
    private var binaryAssetsConfig: TaskConfig? = null

    fun textResources(action: Action<TaskConfig>) {
        if (textResourcesConfig == null) {
            textResourcesConfig = objects.newInstance(TaskConfig::class.java).apply {
                packageName.convention("io.knative.resources")
                resultObjectName.convention("R")
                sourceRoot.convention("commonMain")
                resourcesDir.convention("resources")
            }
        }
        action.execute(textResourcesConfig!!)
    }

    fun binaryAssets(action: Action<TaskConfig>) {
        if (binaryAssetsConfig == null) {
            binaryAssetsConfig = objects.newInstance(TaskConfig::class.java).apply {
                packageName.convention("io.knative.assets")
                resultObjectName.convention("Assets")
                sourceRoot.convention("commonMain")
                resourcesDir.convention("composeResources/files")
            }
        }
        action.execute(binaryAssetsConfig!!)
    }

    internal fun getTextResourcesConfig(): TaskConfig? = textResourcesConfig
    internal fun getBinaryAssetsConfig(): TaskConfig? = binaryAssetsConfig
}