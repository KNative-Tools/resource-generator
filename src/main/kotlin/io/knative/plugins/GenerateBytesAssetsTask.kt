package io.knative.plugins

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.zip.GZIPOutputStream

/**
 * Configuration for a single binary assets generation
 */
data class BinaryAssetsConfig(
    val resourcesDir: File,
    val outputDir: File,
    val packageName: String,
    val resultObjectName: String,
    val includedExtensions: Set<String>,
    val compress: Boolean
)

abstract class GenerateBytesAssetsTask : DefaultTask() {

    @get:Input
    abstract val configs: ListProperty<BinaryAssetsConfig>

    private data class DirNode(
        val name: String,
        val originalName: String,
        val path: String,
        val files: MutableList<File> = mutableListOf(),
        val children: MutableMap<String, DirNode> = mutableMapOf()
    )

    @TaskAction
    fun generate() {
        val configList = configs.get()

        if (configList.isEmpty()) {
            logger.warn("No configurations provided for GenerateBytesAssetsTask")
            return
        }

        // Process each configuration
        configList.forEach { config ->
            generateForConfig(config)
        }
    }

    private fun generateForConfig(config: BinaryAssetsConfig) {
        val outputDirFile = config.outputDir
        outputDirFile.mkdirs()

        // Clean up old generated files
        outputDirFile.listFiles()?.forEach { it.delete() }

        val rootDir = config.resourcesDir

        // Build Tree
        val rootNode = DirNode(config.resultObjectName, "", "")

        rootDir.walkTopDown()
            .filter { it.isFile }
            .filter {
                val extensions = config.includedExtensions
                if (extensions.isEmpty()) true else it.extension in extensions
            }
            .forEach { file ->
                val relPath = file.relativeTo(rootDir).parent ?: ""
                val segments = if (relPath.isEmpty()) emptyList() else relPath.split(File.separator)

                var currentNode = rootNode
                var currentPath = ""

                segments.forEach { segment ->
                    val safeName = sanitizeClassName(segment)
                    currentPath = if(currentPath.isEmpty()) segment else "$currentPath/$segment"
                    currentNode = currentNode.children.getOrPut(safeName) {
                        DirNode(safeName, segment, currentPath)
                    }
                }
                currentNode.files.add(file)
            }

        // Generate the main interface file
        generateMainInterfaceFile(outputDirFile, config)

        // Generate files for all nodes (directories and files)
        generateNodeFiles(rootNode, outputDirFile, "", config)
    }

    private fun generateMainInterfaceFile(outputDirFile: File, config: BinaryAssetsConfig) {
        val sb = StringBuilder()
        val objName = config.resultObjectName

        sb.appendLine("package ${config.packageName}")
        sb.appendLine()
        sb.appendLine("import kotlin.io.encoding.Base64")
        sb.appendLine("import kotlin.io.encoding.ExperimentalEncodingApi")
        sb.appendLine()
        sb.appendLine("/** Auto-generated. Do not modify. */")
        sb.appendLine("@OptIn(ExperimentalEncodingApi::class)")
        sb.appendLine()
        sb.appendLine("/** Extension function to decode Base64 */")
        sb.appendLine("fun String.decoded(): ByteArray = Base64.decode(this)")
        sb.appendLine()
        sb.appendLine("/** Interface for all ${objName} items (files and directories) */")
        sb.appendLine("interface ${objName}Item {")
        sb.appendLine("    fun name(): String")
        sb.appendLine("    fun isDirectory(): Boolean")
        sb.appendLine("    fun listItems(): List<${objName}Item>")
        sb.appendLine("    fun get(fileName: String): ${objName}Item?")
        sb.appendLine("    fun getEncodedData(): String?")
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("/** Extension function to get decoded content from an item */")
        sb.appendLine("fun ${objName}Item.getDecoded(fileName: String): ByteArray? {")
        sb.appendLine("    val item = this.get(fileName) ?: return null")
        sb.appendLine("    return item.getEncodedData()?.decoded()")
        sb.appendLine("}")

        val outputFile = outputDirFile.resolve("${objName}.kt")
        outputFile.writeText(sb.toString())
    }

    private fun generateNodeFiles(node: DirNode, outputDirFile: File, pathPrefix: String, config: BinaryAssetsConfig) {
        // Generate file objects for all files in this node
        node.files.forEach { file ->
            val fileName = sanitizeClassName(file.nameWithoutExtension) + sanitizeClassName(file.extension).replaceFirstChar { it.uppercase() }
            val fullObjectName = if (pathPrefix.isEmpty()) fileName else pathPrefix + fileName
            generateFileObject(file, fullObjectName, outputDirFile, config)
        }

        // Generate directory objects
        node.children.forEach { (childName, childNode) ->
            val fullObjectName = if (pathPrefix.isEmpty()) childName else pathPrefix + childName
            generateDirectoryObject(childNode, fullObjectName, outputDirFile, config)

            // Recursively generate files for child nodes
            generateNodeFiles(childNode, outputDirFile, fullObjectName, config)
        }

        // Generate root object
        if (pathPrefix.isEmpty()) {
            generateRootObject(node, outputDirFile, config)
        }
    }

    private fun generateFileObject(file: File, objectName: String, outputDirFile: File, config: BinaryAssetsConfig) {
        val sb = StringBuilder()
        val objName = config.resultObjectName

        sb.appendLine("package ${config.packageName}")
        sb.appendLine()
        sb.appendLine("/** Auto-generated. Do not modify. */")

        val originalBytes = file.readBytes()
        val finalBytes = if (config.compress) {
            val baos = ByteArrayOutputStream()
            GZIPOutputStream(baos).use { it.write(originalBytes) }
            baos.toByteArray()
        } else {
            originalBytes
        }
        val base64 = Base64.getEncoder().encodeToString(finalBytes)

        sb.appendLine("object $objectName : ${objName}Item {")
        sb.appendLine("    private val encodedData = \"$base64\"")
        sb.appendLine()
        sb.appendLine("    /**")
        sb.appendLine("     * File: ${file.name}")
        sb.appendLine("     * Size: ${originalBytes.size} bytes${if(config.compress) " (Compressed: ${finalBytes.size})" else ""}")
        sb.appendLine("     */")
        sb.appendLine("    override fun name(): String = \"${file.name}\"")
        sb.appendLine("    override fun isDirectory(): Boolean = false")
        sb.appendLine("    override fun listItems(): List<${objName}Item> = emptyList()")
        sb.appendLine("    override fun get(fileName: String): ${objName}Item? = if (fileName == name()) this else null")
        sb.appendLine("    override fun getEncodedData(): String = encodedData")
        sb.appendLine("}")

        val outputFile = outputDirFile.resolve("$objectName.kt")
        outputFile.writeText(sb.toString())
    }

    private fun generateDirectoryObject(node: DirNode, objectName: String, outputDirFile: File, config: BinaryAssetsConfig) {
        val sb = StringBuilder()
        val objName = config.resultObjectName

        sb.appendLine("package ${config.packageName}")
        sb.appendLine()
        sb.appendLine("/** Auto-generated. Do not modify. */")
        sb.appendLine("/** Directory: ${node.path} */")
        sb.appendLine("object $objectName : ${objName}Item {")
        sb.appendLine("    override fun name(): String = \"${node.originalName}\"")
        sb.appendLine("    override fun isDirectory(): Boolean = true")
        sb.appendLine("    override fun getEncodedData(): String? = null")
        sb.appendLine()
        sb.appendLine("    override fun listItems(): List<${objName}Item> = listOf(")

        // List all child file objects
        node.files.forEach { file ->
            val fileName = sanitizeClassName(file.nameWithoutExtension) + sanitizeClassName(file.extension).replaceFirstChar { it.uppercase() }
            val fullObjectName = objectName + fileName
            sb.appendLine("        $fullObjectName,")
        }

        // List all child directory objects
        node.children.forEach { (childName, _) ->
            val fullChildName = objectName + childName
            sb.appendLine("        $fullChildName,")
        }

        sb.appendLine("    )")
        sb.appendLine()
        sb.appendLine("    override fun get(fileName: String): ${objName}Item? = when (fileName) {")

        // Map file names to their objects
        node.files.forEach { file ->
            val fileName = sanitizeClassName(file.nameWithoutExtension) + sanitizeClassName(file.extension).replaceFirstChar { it.uppercase() }
            val fullObjectName = objectName + fileName
            sb.appendLine("        \"${file.name}\" -> $fullObjectName")
        }

        // Map directory names to their objects
        node.children.forEach { (childName, childNode) ->
            val fullChildName = objectName + childName
            sb.appendLine("        \"${childNode.originalName}\" -> $fullChildName")
        }

        sb.appendLine("        else -> null")
        sb.appendLine("    }")
        sb.appendLine("}")

        val outputFile = outputDirFile.resolve("$objectName.kt")
        outputFile.writeText(sb.toString())
    }

    private fun generateRootObject(node: DirNode, outputDirFile: File, config: BinaryAssetsConfig) {
        val sb = StringBuilder()
        val objName = config.resultObjectName

        sb.appendLine("package ${config.packageName}")
        sb.appendLine()
        sb.appendLine("/** Auto-generated. Do not modify. */")
        sb.appendLine("/** Root object for accessing all assets */")
        sb.appendLine("object ${objName}Root : ${objName}Item {")
        sb.appendLine("    override fun name(): String = \"\"")
        sb.appendLine("    override fun isDirectory(): Boolean = true")
        sb.appendLine("    override fun getEncodedData(): String? = null")
        sb.appendLine()
        sb.appendLine("    override fun listItems(): List<${objName}Item> = listOf(")

        // List all root file objects
        node.files.forEach { file ->
            val fileName = sanitizeClassName(file.nameWithoutExtension) + sanitizeClassName(file.extension).replaceFirstChar { it.uppercase() }
            sb.appendLine("        $fileName,")
        }

        // List all root directory objects
        node.children.keys.forEach { childName ->
            sb.appendLine("        $childName,")
        }

        sb.appendLine("    )")
        sb.appendLine()
        sb.appendLine("    override fun get(fileName: String): ${objName}Item? = when (fileName) {")

        // Map file names to their objects
        node.files.forEach { file ->
            val fileName = sanitizeClassName(file.nameWithoutExtension) + sanitizeClassName(file.extension).replaceFirstChar { it.uppercase() }
            sb.appendLine("        \"${file.name}\" -> $fileName")
        }

        // Map directory names to their objects
        node.children.forEach { (childName, childNode) ->
            sb.appendLine("        \"${childNode.originalName}\" -> $childName")
        }

        sb.appendLine("        else -> null")
        sb.appendLine("    }")
        sb.appendLine("}")

        val outputFile = outputDirFile.resolve("${objName}Root.kt")
        outputFile.writeText(sb.toString())
    }


    private fun sanitizeClassName(name: String): String {
         return name.replace(Regex("[^A-Za-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .lowercase(Locale.ROOT)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }
    }
}
