package io.knative.plugins

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.*
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
        task.resourcesDir.set(resourcesDir)
        task.outputDir.set(outputDir)
        task.packageName.set("io.knative.webview.resources")
        task.resultObjectName.set("Assets")
    }

    @Test
    fun `should generate Assets file with base64 encoded content`() {
        val testFile = File(resourcesDir, "test.txt")
        testFile.writeText("Hello World")

        task.generate()

        val outputFile = File(outputDir, "Assets.kt")
        assertTrue(outputFile.exists(), "Assets.kt file should be generated")

        val content = outputFile.readText()
        assertContains(content, "package io.knative.webview.resources")
        assertContains(content, "import kotlin.io.encoding.Base64")
        assertContains(content, "@OptIn(ExperimentalEncodingApi::class)")
        assertContains(content, "object Assets {")
        assertContains(content, "const val TEST_TXT_BASE64: String =")
        assertContains(content, "val TEST_TXT: ByteArray")
        assertContains(content, "get() = Base64.decode(TEST_TXT_BASE64)")
    }

    @Test
    fun `should encode binary content correctly`() {
        val binaryData = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val imageFile = File(resourcesDir, "image.png")
        imageFile.writeBytes(binaryData)

        task.generate()

        val outputFile = File(outputDir, "Assets.kt")
        val content = outputFile.readText()

        val expectedBase64 = Base64.getEncoder().encodeToString(binaryData)
        assertContains(content, "const val IMAGE_PNG_BASE64: String = \"$expectedBase64\"")
        assertContains(content, "val IMAGE_PNG: ByteArray")
    }

    @Test
    fun `should handle nested directories`() {
        val nestedDir = File(resourcesDir, "images/icons")
        nestedDir.mkdirs()
        val iconFile = File(nestedDir, "logo.svg")
        iconFile.writeText("<svg></svg>")

        task.generate()

        val outputFile = File(outputDir, "Assets.kt")
        val content = outputFile.readText()
        assertContains(content, "const val IMAGES_ICONS_LOGO_SVG_BASE64: String =")
        assertContains(content, "val IMAGES_ICONS_LOGO_SVG: ByteArray")
    }

    @Test
    fun `should handle multiple files of any type`() {
        File(resourcesDir, "text.txt").writeText("Text content")
        File(resourcesDir, "image.png").writeBytes(byteArrayOf(1, 2, 3, 4))
        File(resourcesDir, "data.json").writeText("{\"key\":\"value\"}")
        File(resourcesDir, "font.ttf").writeBytes(byteArrayOf(5, 6, 7, 8))
        File(resourcesDir, "archive.zip").writeBytes(byteArrayOf(9, 10, 11, 12))

        task.generate()

        val outputFile = File(outputDir, "Assets.kt")
        val content = outputFile.readText()

        assertContains(content, "TEXT_TXT_BASE64")
        assertContains(content, "IMAGE_PNG_BASE64")
        assertContains(content, "DATA_JSON_BASE64")
        assertContains(content, "FONT_TTF_BASE64")
        assertContains(content, "ARCHIVE_ZIP_BASE64")
    }

    @Test
    fun `should include file documentation`() {
        val testFile = File(resourcesDir, "test.dat")
        val testData = byteArrayOf(1, 2, 3, 4, 5)
        testFile.writeBytes(testData)

        task.generate()

        val outputFile = File(outputDir, "Assets.kt")
        val content = outputFile.readText()

        assertContains(content, "/**")
        assertContains(content, "* File: test.dat")
        assertContains(content, "* Size: 5 bytes")
        assertContains(content, "*/")
    }

    @Test
    fun `should use custom package name and object name`() {
        task.packageName.set("com.example.resources")
        task.resultObjectName.set("BinaryAssets")

        val testFile = File(resourcesDir, "data.bin")
        testFile.writeBytes(byteArrayOf(1, 2, 3))

        task.generate()

        val outputFile = File(outputDir, "BinaryAssets.kt")
        assertTrue(outputFile.exists(), "BinaryAssets.kt file should be generated")

        val content = outputFile.readText()
        assertContains(content, "package com.example.resources")
        assertContains(content, "object BinaryAssets {")
    }

    @Test
    fun `should handle empty resources directory`() {
        task.generate()

        val outputFile = File(outputDir, "Assets.kt")
        assertTrue(outputFile.exists())

        val content = outputFile.readText()
        assertContains(content, "object Assets {")
        assertContains(content, "}")
    }
}