package io.rss.knative.tools.plugins

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.*

abstract class GenerateRTask : DefaultTask() {

    private val acceptedExtensions = setOf("txt", "md", "js", "html", "css")

    @get:InputDirectory
    abstract val resourcesDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputDirFile = outputDir.get().asFile
        outputDirFile.mkdirs()

        val sb = StringBuilder()
        sb.appendLine("package io.rss.knative.tools.webview.resources")
        sb.appendLine()
        sb.appendLine("/** Auto-generated. Do not modify. */")
        sb.appendLine("object R {")

        resourcesDir.get().asFile.walkTopDown()
            .filter { it.isFile }
            .filter { it.extension in acceptedExtensions }
            .forEach { file ->
                val relPath = file.relativeTo(resourcesDir.get().asFile).invariantSeparatorsPath
                val constName = relPath
                    .replace(Regex("[^A-Za-z0-9_]"), "_")
                    .replace(Regex("_+"), "_")
                    .uppercase(Locale.ROOT)

                val fileContent = file.readLines().joinToString(System.lineSeparator())

                sb.appendLine("    const val $constName: String = \"\"\"\n $fileContent \n\"\"\" ")
            }

        sb.appendLine("}")

        val outputFile = outputDirFile.resolve("R.kt")
        outputFile.writeText(sb.toString())
    }
}