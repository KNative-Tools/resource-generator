# Example Generated Output

This document shows examples of the generated Kotlin files.

## Source Structure

```
resources/
├── index.html (content: "<html>Hello</html>")
├── style.css (content: "body { color: red; }")
└── images/
    ├── logo.png (binary content)
    └── icons/
        └── close.svg (content: "<svg>X</svg>")
```

## Generated Files

### 1. Assets.kt (Main Interface File)

```kotlin
package io.knative.webview.resources

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Auto-generated. Do not modify. */
@OptIn(ExperimentalEncodingApi::class)

/** Extension function to decode Base64 */
fun String.decoded(): ByteArray = Base64.decode(this)

/** Interface for all Assets items (files and directories) */
interface AssetsItem {
    fun name(): String
    fun isDirectory(): Boolean
    fun listItems(): List<AssetsItem>
    fun get(fileName: String): AssetsItem?
    fun getEncodedData(): String?
}

/** Extension function to get decoded content from an item */
fun AssetsItem.getDecoded(fileName: String): ByteArray? {
    val item = this.get(fileName) ?: return null
    return item.getEncodedData()?.decoded()
}
```

### 2. AssetsRoot.kt (Root Object)

```kotlin
package io.knative.webview.resources

/** Auto-generated. Do not modify. */
/** Root object for accessing all assets */
object AssetsRoot : AssetsItem {
    override fun name(): String = ""
    override fun isDirectory(): Boolean = true
    override fun getEncodedData(): String? = null

    override fun listItems(): List<AssetsItem> = listOf(
        IndexHtml,
        StyleCss,
        Images,
    )

    override fun get(fileName: String): AssetsItem? = when (fileName) {
        "index.html" -> IndexHtml
        "style.css" -> StyleCss
        "Images" -> Images
        else -> null
    }
}
```

### 3. IndexHtml.kt (File Object)

```kotlin
package io.knative.webview.resources

/** Auto-generated. Do not modify. */
object IndexHtml : AssetsItem {
    private val encodedData = "PGh0bWw+SGVsbG88L2h0bWw+"

    /**
     * File: index.html
     * Size: 19 bytes
     */
    override fun name(): String = "index.html"
    override fun isDirectory(): Boolean = false
    override fun listItems(): List<AssetsItem> = emptyList()
    override fun get(fileName: String): AssetsItem? = if (fileName == name()) this else null
    override fun getEncodedData(): String = encodedData
}
```

### 4. StyleCss.kt (File Object)

```kotlin
package io.knative.webview.resources

/** Auto-generated. Do not modify. */
object StyleCss : AssetsItem {
    private val encodedData = "Ym9keSB7IGNvbG9yOiByZWQ7IH0="

    /**
     * File: style.css
     * Size: 20 bytes
     */
    override fun name(): String = "style.css"
    override fun isDirectory(): Boolean = false
    override fun listItems(): List<AssetsItem> = emptyList()
    override fun get(fileName: String): AssetsItem? = if (fileName == name()) this else null
    override fun getEncodedData(): String = encodedData
}
```

### 5. Images.kt (Directory Object)

```kotlin
package io.knative.webview.resources

/** Auto-generated. Do not modify. */
/** Directory: images */
object Images : AssetsItem {
    override fun name(): String = "Images"
    override fun isDirectory(): Boolean = true
    override fun getEncodedData(): String? = null

    override fun listItems(): List<AssetsItem> = listOf(
        ImagesLogoPng,
        ImagesIcons,
    )

    override fun get(fileName: String): AssetsItem? = when (fileName) {
        "logo.png" -> ImagesLogoPng
        "Icons" -> ImagesIcons
        else -> null
    }
}
```

### 6. ImagesLogoPng.kt (Nested File Object)

```kotlin
package io.knative.webview.resources

/** Auto-generated. Do not modify. */
object ImagesLogoPng : AssetsItem {
    private val encodedData = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="

    /**
     * File: logo.png
     * Size: 95 bytes
     */
    override fun name(): String = "logo.png"
    override fun isDirectory(): Boolean = false
    override fun listItems(): List<AssetsItem> = emptyList()
    override fun get(fileName: String): AssetsItem? = if (fileName == name()) this else null
    override fun getEncodedData(): String = encodedData
}
```

### 7. ImagesIcons.kt (Nested Directory Object)

```kotlin
package io.knative.webview.resources

/** Auto-generated. Do not modify. */
/** Directory: images/icons */
object ImagesIcons : AssetsItem {
    override fun name(): String = "Icons"
    override fun isDirectory(): Boolean = true
    override fun getEncodedData(): String? = null

    override fun listItems(): List<AssetsItem> = listOf(
        ImagesIconsCloseSvg,
    )

    override fun get(fileName: String): AssetsItem? = when (fileName) {
        "close.svg" -> ImagesIconsCloseSvg
        else -> null
    }
}
```

### 8. ImagesIconsCloseSvg.kt (Deeply Nested File Object)

```kotlin
package io.knative.webview.resources

/** Auto-generated. Do not modify. */
object ImagesIconsCloseSvg : AssetsItem {
    private val encodedData = "PHN2Zz5YPC9zdmc+"

    /**
     * File: close.svg
     * Size: 11 bytes
     */
    override fun name(): String = "close.svg"
    override fun isDirectory(): Boolean = false
    override fun listItems(): List<AssetsItem> = emptyList()
    override fun get(fileName: String): AssetsItem? = if (fileName == name()) this else null
    override fun getEncodedData(): String = encodedData
}
```

## Usage Examples

### Example 1: Access Root Files

```kotlin
// Get decoded content of index.html
val htmlBytes = AssetsRoot.getDecoded("index.html")
val htmlContent = htmlBytes?.toString(Charsets.UTF_8)
println(htmlContent) // Output: <html>Hello</html>

// List all root items
val rootItems = AssetsRoot.listItems()
rootItems.forEach { item ->
    println("${item.name()} - isDirectory: ${item.isDirectory()}")
}
// Output:
// index.html - isDirectory: false
// style.css - isDirectory: false
// Images - isDirectory: true
```

### Example 2: Navigate Directory Tree

```kotlin
// Get the Images directory
val imagesDir = AssetsRoot.get("Images")
if (imagesDir?.isDirectory() == true) {
    val imageItems = imagesDir.listItems()
    imageItems.forEach { item ->
        println("${item.name()} - isDirectory: ${item.isDirectory()}")
    }
}

// Navigate deeper
val iconsDir = imagesDir?.get("Icons")
val closeSvg = iconsDir?.get("close.svg")
```

### Example 3: Direct Access to Nested Files

```kotlin
// Access file objects directly by their generated names
val logoData = ImagesLogoPng.getEncodedData()
val logoBytes = logoData?.decoded()

// Or navigate through the tree
val logoBytes2 = AssetsRoot.get("Images")
    ?.get("logo.png")
    ?.getEncodedData()
    ?.decoded()

// Or use the convenience extension
val logoBytes3 = AssetsRoot.get("Images")?.getDecoded("logo.png")
```

### Example 4: Check File Properties

```kotlin
val indexHtml = AssetsRoot.get("index.html")
println(indexHtml?.name()) // Output: index.html
println(indexHtml?.isDirectory()) // Output: false

// Get the encoded data
val encodedData = indexHtml?.getEncodedData()
if (encodedData != null) {
    val decodedBytes = encodedData.decoded()
    val content = decodedBytes.toString(Charsets.UTF_8)
    println(content) // Output: <html>Hello</html>
}
```

## Benefits

1. **Small, manageable files**: Each generated Kotlin file is small and focused
2. **Type-safe access**: Direct object access with compile-time checking
3. **Tree navigation**: Intuitive directory tree navigation
4. **Flexible access patterns**: Support for both direct access and tree traversal
5. **Multiplatform compatible**: Uses only Kotlin standard library (no JVM-specific APIs)
6. **Auto-generated documentation**: Each file includes metadata about the original source
7. **Simple interface**: Single interface for both files and directories, with `getEncodedData()` returning `null` for directories

