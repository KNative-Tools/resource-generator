# Resource Generator Plugin Usage

This project uses the `io.rss.knative.tools.resource-generator` plugin to embed resources (text and binary files) directly into the Kotlin code as generated objects. This is particularly useful for Kotlin Native or multiplatform projects where accessing filesystem resources at runtime might be complex or when you want to bundle assets into the executable.

## 1. Gradle Configuration

The plugin is applied in `build.gradle.kts`. You can configure `textResources` for plain text files and `binaryAssets` for binary files (or files you want to access as `ByteArray`).

### Example: Core Module (`core/build.gradle.kts`)

In the `core` module, both text resources and binary assets are configured.

```kotlin
plugins {
    id("io.rss.knative.tools.resource-generator") version "1.0.0"
}

resourceGenerator {
    // Configuration for text resources (e.g., SQL schemas)
    textResources {
        packageName.set("com.example.petshop.gen")
        resultObjectName.set("R")
        resourcesDir.set("resources/plain")
    }

    // Configuration for binary assets (e.g., initial data)
    binaryAssets {
        packageName.set("com.example.petshop.gen")
        resultObjectName.set("BinAssets")
        sourceRoot.set("commonMain")
        resourcesDir.set("resources/binary")
    }
}
```

### Example: View Module (`view/build.gradle.kts`)

In the `view` module, it is used to bundle web assets (HTML, JS).

```kotlin
resourceGenerator {
    binaryAssets {
        packageName.set("com.example.petshop.gen")
        resultObjectName.set("WebAssets")
        sourceRoot.set("commonMain")
        resourcesDir.set("resources/www")
    }
}
```

## 2. Generated Code

The plugin generates Kotlin objects with properties corresponding to the files in the specified directories. The property names are derived from the filenames (converted to uppercase snake_case).

### Text Resources (`R`)

For `textResources`, the plugin generates a `String` property containing the file content.

**Source File:** `resources/plain/db_schema.sql`
**Generated Object:** `com.example.petshop.gen.R`

```kotlin
package com.example.petshop.gen

/** Auto-generated. Do not modify. */
object R {
    val DB_SCHEMA_SQL: String = $$"""-- Projects table
CREATE TABLE IF NOT EXISTS projects (
    ...
);
...
"""
}
```

### Binary Assets (`WebAssets`, `BinAssets`)

For `binaryAssets`, the plugin generates:
1.  A `const val` with the `_BASE64` suffix containing the Base64 encoded content.
2.  A `val` property (without suffix) that decodes the Base64 string into a `ByteArray`.

**Source File:** `resources/www/dashboard.html`
**Generated Object:** `com.example.petshop.gen.WebAssets`

```kotlin
package com.example.petshop.gen

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Auto-generated. Do not modify. */
@OptIn(ExperimentalEncodingApi::class)
object WebAssets {
    /**
     * File: dashboard.html
     * Size: 13653 bytes
     */
    const val DASHBOARD_HTML_BASE64: String = "PCFET0NUWVBFIGh0bWw+..."

    val DASHBOARD_HTML: ByteArray
        get() = Base64.decode(DASHBOARD_HTML_BASE64)
    
    // ... other files
}
```

## 3. Usage in Kotlin Code

You can access these resources directly in your code.

### Using Text Resources

Used in `core/src/appleMain/kotlin/com/example/petshop/standalone/StandaloneTxStreamPetApp.kt`:

```kotlin
import com.example.petshop.gen.R

fun main() {
    // Accessing the SQL schema string directly
    val ddlList = R.DB_SCHEMA_SQL.split(";")
        .map { def -> def.lines().filter { !it.startsWith("--") }.joinToString("\n")}
        .filter { it.trim().isNotEmpty() }
        .map { it.trim() }
    
    // ...
}
```

### Using Binary Assets

Used in `app/src/appleMain/kotlin/com/example/petshop/WebRoutes.kt` to serve web content:

```kotlin
import com.example.petshop.gen.WebAssets
import io.ktor.http.ContentType

private val RESOURCES = mapOf(
    // Accessing the ByteArray property
    "" to WebResource(WebAssets.DASHBOARD_HTML, ContentType.Text.Html),
    "alpine.js" to WebResource(WebAssets.ALPINE_JS, ContentType.Application.JavaScript),
    // ...
)
```

Used in `app/src/appleMain/kotlin/com/example/petshop/AppConfigurer.kt` (decoding manually if needed, though the generated object provides a ByteArray accessor):

```kotlin
import com.example.petshop.gen.BinAssets

// ...
val sqlList = cleanUpSqlSource(R.DB_SCHEMA_SQL)
    .plus(cleanUpSqlSource(BinAssets.VULNERABILITIES_MIN_LOAD_SQL.decodeToString()))
// ...
```
