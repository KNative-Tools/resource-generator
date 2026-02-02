# Resource Generator Plugin

The **Resource Generator Plugin** is a Gradle plugin designed to simplify resource management in Kotlin Multiplatform projects, specifically targeting **Kotlin Native** (Desktop).

It allows you to embed resources (text files, images, web assets, etc.) directly into your Kotlin code as generated objects. This ensures that your resources are compiled into your application binary, making them easy to distribute and access without relying on external filesystem paths at runtime.

## Documentation

For detailed usage instructions, examples, and configuration options, please visit the [documentation website](https://kmupla.github.io/resource-generator/).

## Overview

In the JVM ecosystem, accessing resources is straightforward using standard Java mechanisms. However, in Native development, distributing auxiliary files and accessing them reliably across different operating systems can be complex.

This plugin bridges that gap by **embedding the resources directly into the source code**, inspired by the Android `R` object pattern.

## Architecture

The plugin operates in a few high-level steps to integrate resources into your application:

1.  **Configuration**: You define resource directories and naming conventions in your build script.
2.  **Generation**: The plugin scans the specified directories during the build process.
3.  **Embedding**: It generates Kotlin files where:
    - Text files are embedded as string constants.
    - Binary files are encoded and accessible as `ByteArray`s.
4.  **Compilation**: These generated files are compiled with your project, ensuring resources are available at runtime without external dependencies.

## Contributing

Contributions are welcome!

If you would like to contribute to the project, please **create a Pull Request** with your changes. We prefer Pull Requests over opening Issues for proposing changes or fixes.

## License

This project is licensed under the Apache License, Version 2.0.
