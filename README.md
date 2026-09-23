# Sprout 🌱

[![Build & Test](https://github.com/arschaos/sprout/actions/workflows/maven.yaml/badge.svg)](https://github.com/arschaos/sprout/actions/workflows/maven.yaml)
[![Maven Central](https://img.shields.io/maven-central/v/com.arschaos/sprout-maven-plugin.svg)](https://central.sonatype.com/artifact/com.arschaos/sprout-maven-plugin)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Sprout** is a lightweight, near-instant architecture visualization engine for Java and Spring Boot projects. It runs as a Maven plugin, statically analyzing your source code to detect components and their architectural relationships, generating an interactive dashboard and exportable diagrams in seconds.

---

## Features

- ⚡ **Zero-Configuration Static Analysis**: Analyzes source code using AST parsing without needing to compile or run the application bytecode.
- 🧩 **Spring Boot Component Detection**: Automatically classifies controllers, services, repositories, configurations, records, entities/models, and components.
- 🔗 **Relationship Extraction**: Maps class inheritance (`EXTENDS`), interface implementation (`IMPLEMENTS`), dependency injection (`INJECTS` via `@Autowired` or Lombok constructors), field usage (`USES`), and method call links (`CALLS`).
- 🖥️ **Interactive Web Visualizer**: Launches a local dashboard serving interactive graphs with search, zoom, filter, and component inspection.
- 📊 **Multi-Format Export**: Generates standalone HTML dashboards, raw JSON graph representations (`architecture.json`), and Mermaid-compatible diagram data.

---

## Quick Start

### Option 1: Run directly with Maven CLI (No `pom.xml` changes needed)

You can run Sprout on any Maven project immediately:

```bash
mvn com.arschaos:sprout-maven-plugin:0.1.0:generate
```

### Option 2: Add to your `pom.xml`

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.arschaos</groupId>
            <artifactId>sprout-maven-plugin</artifactId>
            <version>0.1.0</version>
        </plugin>
    </plugins>
</build>
```

Then run:

```bash
mvn sprout:generate
```

Sprout will analyze the codebase, write generated diagram assets to `target/sprout/`, and automatically launch the interactive visualizer in your default browser at `http://localhost:8383/`.

---

## Configuration

Configure Sprout via plugin `<configuration>` or command-line properties:

```xml
<plugin>
    <groupId>com.arschaos</groupId>
    <artifactId>sprout-maven-plugin</artifactId>
    <version>0.1.0</version>
    <configuration>
        <!-- Custom port (default: 8383) -->
        <port>9090</port>
        <!-- Disable automatic browser launch (default: true) -->
        <launchApp>true</launchApp>
        <!-- Custom output directory (default: target/sprout) -->
        <outputDirectory>${project.build.directory}/sprout</outputDirectory>
    </configuration>
</plugin>
```

### Command Line Flags

| Property | Default | Description | Example |
|---|---|---|---|
| `-Dsprout.launchApp` | `true` | Open the interactive web visualizer in browser | `-Dsprout.launchApp=false` |
| `-Dsprout.port` | `8383` | Port for the embedded visualizer server | `-Dsprout.port=9090` |
| `-Dsprout.outputDir` | `target/sprout` | Destination folder for diagram artifacts | `-Dsprout.outputDir=docs/arch` |
| `-Dsprout.keepAlive` | `false` | Keep server running in background daemon mode | `-Dsprout.keepAlive=true` |

### Configuration File (`sprout.properties`)

You can also place a `sprout.properties` file in the project root or in `src/main/resources/`:

```properties
sprout.port=8585
```

---

## Generated Artifacts & Visualizer API

When executed, Sprout generates artifacts in `target/sprout/`:

- `index.html`, `dashboard.css`, `dashboard.js`: Standalone interactive visualization app.
- `architecture.json`: Raw node/edge graph data with metrics.

When the visualizer server is running, the following REST endpoints are available:

- `GET /` — Interactive web dashboard
- `GET /api/graph` — Complete architectural graph JSON
- `GET /api/metrics` — Component statistics and analysis metrics
- `GET /api/health` — Visualizer server health status

---

## Requirements

- **Java**: 17 or higher
- **Maven**: 3.6 or higher

---

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).
