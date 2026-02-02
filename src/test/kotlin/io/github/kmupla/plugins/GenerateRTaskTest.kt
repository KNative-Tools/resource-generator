package io.github.kmupla.plugins

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Disabled
class GenerateRTaskTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var resourcesDir: File
    private lateinit var outputDir: File
    private lateinit var task: GenerateRTask

    @BeforeEach
    fun setup() {
        resourcesDir = File(tempDir, "resources")
        outputDir = File(tempDir, "output")
        resourcesDir.mkdirs()
        outputDir.mkdirs()

        val project = ProjectBuilder.builder().build()
        task = project.tasks.create("testGenerateR", GenerateRTask::class.java)
        task.resourcesDir.set(resourcesDir)
        task.outputDir.set(outputDir)
        task.packageName.set("io.knative.webview.resources")
        task.resultObjectName.set("R")
    }

    @Test
    fun `should generate R file with text content`() {
        val testFile = File(resourcesDir, "test.txt")
        testFile.writeText("Hello World\nThis is a test file")

        task.generate()

        // Then
        val outputFile = File(outputDir, "R.kt")
        assertTrue(outputFile.exists(), "R.kt file should be generated")

        val content = outputFile.readText()
        assertContains(content, "package io.knative.webview.resources")
        assertContains(content, "object R {")
        assertContains(content, "const val TEST_TXT: String = \"\"\"")
        assertContains(content, "Hello World")
        assertContains(content, "This is a test file")
    }

    @Test
    fun `should generate R file with HTML content`() {
        val htmlFile = File(resourcesDir, "page.html")
        htmlFile.writeText(
            """
            <!DOCTYPE html>
            <html>
            <head><title>Test</title></head>
            <body><h1>Hello</h1></body>
            </html>
        """.trimIndent()
        )

        task.generate()

        // Then
        val outputFile = File(outputDir, "R.kt")
        val content = outputFile.readText()
        assertContains(content, "const val PAGE_HTML: String = \"\"\"")
        assertContains(content, "<!DOCTYPE html>")
        assertContains(content, "<title>Test</title>")
    }

    @Test
    fun `should handle nested directories`() {
        val nestedDir = File(resourcesDir, "web/css")
        nestedDir.mkdirs()
        val cssFile = File(nestedDir, "style.css")
        cssFile.writeText("body { color: red; }")

        task.generate()

        // Then
        val outputFile = File(outputDir, "R.kt")
        val content = outputFile.readText()
        assertContains(content, "const val WEB_CSS_STYLE_CSS: String = \"\"\"")
        assertContains(content, "body { color: red; }")
    }

    @Test
    fun `should handle multiple files of different types`() {
        File(resourcesDir, "text.txt").writeText("Text content")
        File(resourcesDir, "script.js").writeText("console.log('test');")
        File(resourcesDir, "page.html").writeText("<html></html>")
        File(resourcesDir, "style.css").writeText("body{}")
        File(resourcesDir, "readme.md").writeText("# Title")

        task.generate()

        // Then
        val outputFile = File(outputDir, "R.kt")
        val content = outputFile.readText()

        assertContains(content, "const val TEXT_TXT: String = \"\"\"")
        assertContains(content, "const val SCRIPT_JS: String = \"\"\"")
        assertContains(content, "const val PAGE_HTML: String = \"\"\"")
        assertContains(content, "const val STYLE_CSS: String = \"\"\"")
        assertContains(content, "const val README_MD: String = \"\"\"")

        assertContains(content, "Text content")
        assertContains(content, "console.log('test');")
        assertContains(content, "<html></html>")
        assertContains(content, "body{}")
        assertContains(content, "# Title")
    }

    @Test
    fun `should ignore unsupported file extensions`() {
        File(resourcesDir, "supported.txt").writeText("Include this")
        File(resourcesDir, "ignored.png").writeBytes(byteArrayOf(1, 2, 3))
        File(resourcesDir, "also-ignored.jpg").writeBytes(byteArrayOf(4, 5, 6))
        File(resourcesDir, "config.json").writeText("{}")

        task.generate()

        // Then
        val outputFile = File(outputDir, "R.kt")
        val content = outputFile.readText()

        assertContains(content, "const val SUPPORTED_TXT: String = \"\"\"")
        assertContains(content, "Include this")

        // Should not contain unsupported files
        assertTrue(!content.contains("IGNORED_PNG"))
        assertTrue(!content.contains("ALSO_IGNORED_JPG"))
        assertTrue(!content.contains("CONFIG_JSON"))
    }

    @Test
    fun `should sanitize file names to valid constant names`() {
        File(resourcesDir, "file-with-dashes.txt").writeText("Content")
        File(resourcesDir, "file.with.dots.txt").writeText("More content")
        File(resourcesDir, "file with spaces.txt").writeText("Space content")
        File(resourcesDir, "file__multiple___underscores.txt").writeText("Underscore content")

        task.generate()

        // Then
        val outputFile = File(outputDir, "R.kt")
        val content = outputFile.readText()

        assertContains(content, "const val FILE_WITH_DASHES_TXT: String = \"\"\"")
        assertContains(content, "const val FILE_WITH_DOTS_TXT: String = \"\"\"")
        assertContains(content, "const val FILE_WITH_SPACES_TXT: String = \"\"\"")
        assertContains(content, "const val FILE_MULTIPLE_UNDERSCORES_TXT: String = \"\"\"")
    }

    @Test
    fun `should handle empty resource directory`() {

        task.generate()

        // Then
        val outputFile = File(outputDir, "R.kt")
        assertTrue(outputFile.exists())

        val content = outputFile.readText()
        assertContains(content, "package io.knative.webview.resources")
        assertContains(content, "object R {")
        assertContains(content, "}")

        // Should only contain the basic structure
        val lines = content.lines().filter { it.trim().isNotEmpty() }
        assertEquals(4, lines.size) // package, empty line, comment, object declaration, closing brace
    }

    @Test
    fun `should handle files with special characters`() {
        val testFile = File(resourcesDir, "special.txt")
        testFile.writeText("Content with special chars: @#\$%^&*()[]{}|\\:;\"'<>?,./`~")

        task.generate()

        // Then
        val outputFile = File(outputDir, "R.kt")
        val content = outputFile.readText()

        assertContains(content, "const val SPECIAL_TXT: String = \"\"\"")
        // Verify that special characters are properly escaped in the generated code
        assertContains(content, "Content with special chars: @#\\$%^&*()[]{}|\\\\:;\\\"'<>?,./`~")
    }

    @Test
    fun `should handle multiline content correctly`() {
        val testFile = File(resourcesDir, "multiline.txt")
        testFile.writeText(
            """
            Line 1
            Line 2
            Line 3

            Line 5 after empty line
        """.trimIndent()
        )

        task.generate()

        // Then
        val outputFile = File(outputDir, "R.kt")
        val content = outputFile.readText()

        assertContains(content, "const val MULTILINE_TXT: String = \"\"\"")
        assertContains(content, "Line 1")
        assertContains(content, "Line 2")
        assertContains(content, "Line 3")
        assertContains(content, "Line 5 after empty line")
    }

    @Test
    fun `should create output directory if it doesn't exist`() {
        val nonExistentOutput = File(tempDir, "new_output")
        task.outputDir.set(nonExistentOutput)
        File(resourcesDir, "test.txt").writeText("Content")

        task.generate()

        // Then
        assertTrue(nonExistentOutput.exists(), "Output directory should be created")
        val outputFile = File(nonExistentOutput, "R.kt")
        assertTrue(outputFile.exists(), "R.kt should be created in new directory")
    }
}