package io.knative.plugins

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.zip.GZIPOutputStream
import kotlin.test.assertContains
import kotlin.test.assertTrue

class GenerateBytesAssetsTaskTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var resourcesDir: File
    private lateinit var outputDir: File
    private lateinit var task: GenerateBytesAssetsTask

    @BeforeEach
    fun setup() {
        resourcesDir = File(tempDir, "resources")
        outputDir = File(tempDir, "output")
        resourcesDir.mkdirs()
        outputDir.mkdirs()

        val project = ProjectBuilder.builder().build()
        task = project.tasks.create("testGenerateBytesAssets", GenerateBytesAssetsTask::class.java)

        // Use new configs API
        val config = BinaryAssetsConfig(
            resourcesDir = resourcesDir,
            outputDir = outputDir,
            packageName = "io.knative.webview.resources",
            resultObjectName = "Assets",
            includedExtensions = emptySet(),
            compress = false
        )
        task.configs.set(listOf(config))
    }

    @Test
    fun `should generate Assets file with base64 encoded content`() {
        val testFile = File(resourcesDir, "test.txt")
        testFile.writeText("Hello World")

        task.generate()

        // Check main interface file
        val mainFile = File(outputDir, "Assets.kt")
        assertTrue(mainFile.exists(), "Assets.kt file should be generated")

        val mainContent = mainFile.readText()
        assertContains(mainContent, "package io.knative.webview.resources")
        assertContains(mainContent, "import kotlin.io.encoding.Base64")
        assertContains(mainContent, "@OptIn(ExperimentalEncodingApi::class)")
        assertContains(mainContent, "fun String.decoded(): ByteArray")
        assertContains(mainContent, "interface AssetsItem {")
        assertContains(mainContent, "fun name(): String")
        assertContains(mainContent, "fun isDirectory(): Boolean")
        assertContains(mainContent, "fun listItems(): List<AssetsItem>")
        assertContains(mainContent, "fun get(fileName: String): AssetsItem?")
        assertContains(mainContent, "fun getEncodedData(): String?")
        assertContains(mainContent, "fun AssetsItem.getDecoded(fileName: String): ByteArray?")

        // Check root object file
        val rootFile = File(outputDir, "AssetsRoot.kt")
        assertTrue(rootFile.exists(), "AssetsRoot.kt file should be generated")
        val rootContent = rootFile.readText()
        assertContains(rootContent, "object AssetsRoot : AssetsItem")
        assertContains(rootContent, "\"test.txt\" -> TestTxt")
        assertContains(rootContent, "override fun listItems(): List<AssetsItem>")

        // Check file object
        val fileObjectFile = File(outputDir, "TestTxt.kt")
        assertTrue(fileObjectFile.exists(), "TestTxt.kt file should be generated")
        val fileContent = fileObjectFile.readText()
        assertContains(fileContent, "object TestTxt : AssetsItem")
        assertContains(fileContent, "SGVsbG8gV29ybGQ=") // Base64 of "Hello World"
        assertContains(fileContent, "override fun name(): String = \"test.txt\"")
        assertContains(fileContent, "override fun isDirectory(): Boolean = false")
        assertContains(fileContent, "override fun getEncodedData(): String = encodedData")
    }

    @Test
    fun `should encode binary content correctly`() {
        val binaryData = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val imageFile = File(resourcesDir, "image.png")
        imageFile.writeBytes(binaryData)

        task.generate()

        val fileObjectFile = File(outputDir, "ImagePng.kt")
        assertTrue(fileObjectFile.exists(), "ImagePng.kt file should be generated")

        val content = fileObjectFile.readText()
        val expectedBase64 = Base64.getEncoder().encodeToString(binaryData)
        assertContains(content, "object ImagePng : AssetsItem")
        assertContains(content, expectedBase64)
        assertContains(content, "override fun name(): String = \"image.png\"")
    }

    @Test
    fun `should handle nested directories`() {
        val nestedDir = File(resourcesDir, "images/icons")
        nestedDir.mkdirs()
        val iconFile = File(nestedDir, "logo.svg")
        iconFile.writeText("<svg></svg>")

        task.generate()

        // Check Images directory object
        val imagesFile = File(outputDir, "Images.kt")
        assertTrue(imagesFile.exists(), "Images.kt file should be generated")
        val imagesContent = imagesFile.readText()
        assertContains(imagesContent, "object Images : AssetsItem")
        assertContains(imagesContent, "override fun isDirectory(): Boolean = true")
        assertContains(imagesContent, "\"icons\" -> ImagesIcons")
        assertContains(imagesContent, "override fun listItems(): List<AssetsItem>")

        // Check ImagesIcons directory object
        val iconsFile = File(outputDir, "ImagesIcons.kt")
        assertTrue(iconsFile.exists(), "ImagesIcons.kt file should be generated")
        val iconsContent = iconsFile.readText()
        assertContains(iconsContent, "object ImagesIcons : AssetsItem")
        assertContains(iconsContent, "override fun isDirectory(): Boolean = true")
        assertContains(iconsContent, "\"logo.svg\" -> ImagesIconsLogoSvg")

        // Check file object
        val logoFile = File(outputDir, "ImagesIconsLogoSvg.kt")
        assertTrue(logoFile.exists(), "ImagesIconsLogoSvg.kt file should be generated")
        val logoContent = logoFile.readText()
        assertContains(logoContent, "object ImagesIconsLogoSvg : AssetsItem")
        assertContains(logoContent, "PHN2Zz48L3N2Zz4=") // Base64 of "<svg></svg>"
        assertContains(logoContent, "override fun name(): String = \"logo.svg\"")
    }

    @Test
    fun `should handle multiple files of any type`() {
        File(resourcesDir, "text.txt").writeText("Text content")
        File(resourcesDir, "image.png").writeBytes(byteArrayOf(1, 2, 3, 4))
        File(resourcesDir, "data.json").writeText("{\"key\":\"value\"}")
        File(resourcesDir, "font.ttf").writeBytes(byteArrayOf(5, 6, 7, 8))
        File(resourcesDir, "archive.zip").writeBytes(byteArrayOf(9, 10, 11, 12))

        task.generate()

        // Check that all file objects exist
        assertTrue(File(outputDir, "TextTxt.kt").exists())
        assertTrue(File(outputDir, "ImagePng.kt").exists())
        assertTrue(File(outputDir, "DataJson.kt").exists())
        assertTrue(File(outputDir, "FontTtf.kt").exists())
        assertTrue(File(outputDir, "ArchiveZip.kt").exists())

        // Check root object references them
        val rootContent = File(outputDir, "AssetsRoot.kt").readText()
        assertContains(rootContent, "\"text.txt\" -> TextTxt")
        assertContains(rootContent, "\"image.png\" -> ImagePng")
        assertContains(rootContent, "\"data.json\" -> DataJson")
        assertContains(rootContent, "\"font.ttf\" -> FontTtf")
        assertContains(rootContent, "\"archive.zip\" -> ArchiveZip")
    }

    @Test
    fun `should include file documentation`() {
        val testFile = File(resourcesDir, "test.dat")
        val testData = byteArrayOf(1, 2, 3, 4, 5)
        testFile.writeBytes(testData)

        task.generate()

        val fileObjectFile = File(outputDir, "TestDat.kt")
        val content = fileObjectFile.readText()

        assertContains(content, "/**")
        assertContains(content, "* File: test.dat")
        assertContains(content, "* Size: 5 bytes")
        assertContains(content, "*/")
    }

    @Test
    fun `should use custom package name and object name`() {
        val customConfig = BinaryAssetsConfig(
            resourcesDir = resourcesDir,
            outputDir = outputDir,
            packageName = "com.example.resources",
            resultObjectName = "BinaryAssets",
            includedExtensions = emptySet(),
            compress = false
        )
        task.configs.set(listOf(customConfig))

        val testFile = File(resourcesDir, "data.bin")
        testFile.writeBytes(byteArrayOf(1, 2, 3))

        task.generate()

        // Check main interface file
        val mainFile = File(outputDir, "BinaryAssets.kt")
        assertTrue(mainFile.exists(), "BinaryAssets.kt file should be generated")

        val mainContent = mainFile.readText()
        assertContains(mainContent, "package com.example.resources")
        assertContains(mainContent, "interface BinaryAssetsItem {")

        // Check root object file
        val rootFile = File(outputDir, "BinaryAssetsRoot.kt")
        assertTrue(rootFile.exists(), "BinaryAssetsRoot.kt file should be generated")
        val rootContent = rootFile.readText()
        assertContains(rootContent, "package com.example.resources")
        assertContains(rootContent, "object BinaryAssetsRoot : BinaryAssetsItem")
    }

    @Test
    fun `should handle empty resources directory`() {
        task.generate()

        // Check main interface file exists
        val mainFile = File(outputDir, "Assets.kt")
        assertTrue(mainFile.exists(), "Assets.kt file should be generated")
        val mainContent = mainFile.readText()
        assertContains(mainContent, "interface AssetsItem {")

        // Check root object file exists
        val rootFile = File(outputDir, "AssetsRoot.kt")
        assertTrue(rootFile.exists(), "AssetsRoot.kt file should be generated")
        val rootContent = rootFile.readText()
        assertContains(rootContent, "object AssetsRoot : AssetsItem")
    }

    @Test
    fun `should compress content when enabled`() {
        val compressConfig = BinaryAssetsConfig(
            resourcesDir = resourcesDir,
            outputDir = outputDir,
            packageName = "io.knative.webview.resources",
            resultObjectName = "Assets",
            includedExtensions = emptySet(),
            compress = true
        )
        task.configs.set(listOf(compressConfig))

        val testFile = File(resourcesDir, "test.txt")
        val originalContent = "Hello World Repeated ".repeat(10)
        testFile.writeText(originalContent)

        task.generate()

        val fileObjectFile = File(outputDir, "TestTxt.kt")
        val content = fileObjectFile.readText()

        val originalBytes = originalContent.toByteArray()
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(originalBytes) }
        val compressedBytes = baos.toByteArray()
        val expectedBase64 = Base64.getEncoder().encodeToString(compressedBytes)

        assertContains(content, "object TestTxt : AssetsItem")
        assertContains(content, expectedBase64)
        assertContains(content, "(Compressed: ${compressedBytes.size})")
    }
}