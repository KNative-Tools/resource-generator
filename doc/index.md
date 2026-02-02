# Resource Generator Plugin

## Overview

The **Resource Generator Plugin** is a Gradle plugin designed to simplify resource management in Kotlin Multiplatform projects, specifically targeting **Kotlin Native** (Desktop).

It allows you to embed resources (text files, images, web assets, etc.) directly into your Kotlin code as generated objects. This ensures that your resources are compiled into your application binary, making them easy to distribute and access without relying on external filesystem paths at runtime.

## Motivation

In the **JVM** ecosystem, accessing resources is straightforward. Developers are accustomed to using mechanisms like `ClassLoader.getResource()`, `getResourceAsStream()`, or accessing the classpath directly.

However, in **Native development** (especially Kotlin Native on desktop), these high-level conveniences are often missing or work differently. Distributing auxiliary files alongside a native binary can be cumbersome, and accessing them reliably across different operating systems adds complexity.

This plugin bridges that gap by **embedding the resources directly into the source code**.

## Inspiration

The design of this plugin is heavily inspired by the **`R` object** pattern found in **Android** development.

- In Android, resources (XML layouts, strings, drawables) are compiled and referenced via a generated `R` class (e.g., `R.string.app_name`, `R.layout.main_activity`).
- This plugin provides a similar experience: resources placed in configured directories are converted into Kotlin Objects (e.g., `Assets`, `R`) with properties representing the file contents.

## How It Works

1.  **Configuration**: You specify which folders contain your resources and how they should be named (package name, object name) in your `build.gradle.kts`.
2.  **Generation**: During the build process, the plugin scans these directories.
3.  **Embedding**: It generates Kotlin files where:
    - **Text files** (e.g., SQL, JSON, Markdown) are embedded as string constants.
    - **Binary files** (e.g., Images, Audio, compiled code) are encoded (e.g., Base64) and accessible as `ByteArray`s.
4.  **Compilation**: These generated files are compiled along with your project, becoming an integral part of the final artifact.
