package io.knative.plugins

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.*

abstract class GenerateBytesAssetsTask : DefaultTask() {

    @get:InputDirectory
    abstract val resourcesDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val resultObjectName: Property<String>

    @TaskAction
    fun generate() {
        val outputDirFile = outputDir.get().asFile
        outputDirFile.mkdirs()

        val sb = StringBuilder()
        sb.appendLine("package ${packageName.get()}")
        sb.appendLine()
        sb.appendLine("import kotlin.io.encoding.Base64")
        sb.appendLine("import kotlin.io.encoding.ExperimentalEncodingApi")
        sb.appendLine()
        sb.appendLine("/** Auto-generated. Do not modify. */")
        sb.appendLine("@OptIn(ExperimentalEncodingApi::class)")
        sb.appendLine("object ${resultObjectName.get()} {")

        resourcesDir.get().asFile.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val relPath = file.relativeTo(resourcesDir.get().asFile).invariantSeparatorsPath
                val constName = relPath
                    .replace(Regex("[^A-Za-z0-9_]"), "_")
                    .replace(Regex("_+"), "_")
                    .uppercase(Locale.ROOT)

                val bytes = file.readBytes()
                val base64 = Base64.getEncoder().encodeToString(bytes)

                sb.appendLine("    /**")
                sb.appendLine("     * File: $relPath")
                sb.appendLine("     * Size: ${bytes.size} bytes")
                sb.appendLine("     */")
                sb.appendLine("    const val ${constName}_BASE64: String = \"$base64\"")
                sb.appendLine()
                sb.appendLine("    val $constName: ByteArray")
                sb.appendLine("        get() = Base64.decode(${constName}_BASE64)")
                sb.appendLine()
            }

        sb.appendLine("}")

        val outputFile = outputDirFile.resolve("${resultObjectName.get()}.kt")
        outputFile.writeText(sb.toString())
    }
}