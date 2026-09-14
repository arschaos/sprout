package io.arschaos;

import io.arschaos.core.SproutEngine;
import io.arschaos.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SproutEngineTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldAnalyzeSpringCodebaseCorrectly() throws IOException {
        Path src = tempDir.resolve("src/main/java/com/example/demo");
        Files.createDirectories(src);

        // Service Interface
        Files.writeString(src.resolve("OrderService.java"),
                "package com.example.demo;\n" +
                "public interface OrderService {\n" +
                "    void processOrder(String id);\n" +
                "}\n");

        // Service Impl
        Files.writeString(src.resolve("OrderServiceImpl.java"),
                "package com.example.demo;\n" +
                "import org.springframework.stereotype.Service;\n" +
                "@Service\n" +
                "public class OrderServiceImpl implements OrderService {\n" +
                "    @Override\n" +
                "    public void processOrder(String id) {}\n" +
                "}\n");

        // Controller
        Files.writeString(src.resolve("OrderController.java"),
                "package com.example.demo;\n" +
                "import org.springframework.web.bind.annotation.RestController;\n" +
                "import lombok.RequiredArgsConstructor;\n" +
                "@RestController\n" +
                "@RequiredArgsConstructor\n" +
                "public class OrderController {\n" +
                "    private final OrderService orderService;\n" +
                "    public void submitOrder() {\n" +
                "        orderService.processOrder(\"123\");\n" +
                "    }\n" +
                "}\n");

        SproutEngine engine = new SproutEngine();
        ArchitectureGraph graph = engine.analyze(tempDir.toFile());

        assertThat(graph).isNotNull();
        assertThat(graph.getNodes()).hasSize(3);

        ComponentNode controllerNode = graph.getNodes().stream()
                .filter(n -> n.getName().equals("OrderController"))
                .findFirst().orElseThrow();
        assertThat(controllerNode.getType()).isEqualTo(ComponentType.CONTROLLER);

        ComponentNode serviceNode = graph.getNodes().stream()
                .filter(n -> n.getName().equals("OrderServiceImpl"))
                .findFirst().orElseThrow();
        assertThat(serviceNode.getType()).isEqualTo(ComponentType.SERVICE);

        ComponentNode interfaceNode = graph.getNodes().stream()
                .filter(n -> n.getName().equals("OrderService"))
                .findFirst().orElseThrow();
        assertThat(interfaceNode.getType()).isEqualTo(ComponentType.INTERFACE);

        // Check Edges
        assertThat(graph.getEdges()).anyMatch(e ->
                e.getSource().equals(controllerNode.getId()) &&
                e.getTarget().equals(interfaceNode.getId()) &&
                e.getType() == RelationshipType.INJECTS);

        assertThat(graph.getEdges()).anyMatch(e ->
                e.getSource().equals(controllerNode.getId()) &&
                e.getTarget().equals(interfaceNode.getId()) &&
                e.getType() == RelationshipType.CALLS);

        assertThat(graph.getEdges()).anyMatch(e ->
                e.getSource().equals(serviceNode.getId()) &&
                e.getTarget().equals(interfaceNode.getId()) &&
                e.getType() == RelationshipType.IMPLEMENTS);

        // Test JSON export
        File jsonFile = tempDir.resolve("target/sprout/architecture.json").toFile();
        engine.exportJson(graph, jsonFile);
        assertThat(jsonFile).exists();
        String jsonContent = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
        assertThat(jsonContent).contains("OrderController", "OrderService", "INJECTS");

        // Test HTML export
        File htmlFile = tempDir.resolve("target/sprout/index.html").toFile();
        File cssFile = tempDir.resolve("target/sprout/dashboard.css").toFile();
        File jsFile = tempDir.resolve("target/sprout/dashboard.js").toFile();
        engine.exportHtml(graph, htmlFile);
        assertThat(htmlFile).exists();
        assertThat(cssFile).exists();
        assertThat(jsFile).exists();

        String htmlContent = Files.readString(htmlFile.toPath(), StandardCharsets.UTF_8);
        assertThat(htmlContent).contains(
                "Sprout Architecture Visualizer",
                "OrderController",
                "dashboard.css",
                "dashboard.js",
                "tab-legend",
                "edge-details",
                "INJECTS",
                "IMPLEMENTS",
                "CALLS",
                "EXTENDS",
                "USES"
        );
        assertThat(htmlContent).doesNotContain("canvas-legend");
        String cssContent = Files.readString(cssFile.toPath(), StandardCharsets.UTF_8);
        assertThat(cssContent).contains("--bg-color", "canvas", "legend-card", "legend-svg");
        String jsContent = Files.readString(jsFile.toPath(), StandardCharsets.UTF_8);
        assertThat(jsContent).contains("initDashboard", "COLOR_MAP", "showEdgeDetails", "highlightEdgeType");

        // Test Summary render
        String summary = engine.formatSummary(graph);
        assertThat(summary).contains("SPROUT ARCHITECTURE ANALYSIS", "OrderController");
    }

    @Test
    void shouldResolvePortFromConfigurationAndProperties() throws IOException {
        SproutEngine engine = new SproutEngine();

        // 1. Explicit port
        assertThat(engine.resolvePort(tempDir.toFile(), 9090)).isEqualTo(9090);

        // 2. Default when no properties file
        assertThat(engine.resolvePort(tempDir.toFile(), null)).isEqualTo(8383);

        // 3. sprout.properties in root
        Files.writeString(tempDir.resolve("sprout.properties"), "sprout.port=9999\n");
        assertThat(engine.resolvePort(tempDir.toFile(), null)).isEqualTo(9999);
    }
}
