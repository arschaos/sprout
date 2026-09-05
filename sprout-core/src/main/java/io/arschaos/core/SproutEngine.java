package io.arschaos.core;

import io.arschaos.model.ArchitectureGraph;
import io.arschaos.renderer.HtmlDashboardRenderer;
import io.arschaos.renderer.JsonExporter;
import io.arschaos.renderer.SummaryTextRenderer;
import io.arschaos.scanner.JavaParserAnalyzer;
import io.arschaos.scanner.JavaSourceScanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Core engine for scanning, parsing, and analyzing Java / Spring Boot codebases.
 */
public class SproutEngine {

    public static final int DEFAULT_PORT = 8383;

    private final JavaSourceScanner scanner;
    private final JavaParserAnalyzer analyzer;
    private final JsonExporter jsonExporter;
    private final HtmlDashboardRenderer htmlRenderer;
    private final SummaryTextRenderer summaryRenderer;

    public SproutEngine() {
        this.scanner = new JavaSourceScanner();
        this.analyzer = new JavaParserAnalyzer();
        this.jsonExporter = new JsonExporter();
        this.htmlRenderer = new HtmlDashboardRenderer();
        this.summaryRenderer = new SummaryTextRenderer();
    }

    public ArchitectureGraph analyze(File projectDir) {
        List<File> javaFiles = scanner.scan(projectDir);
        return analyzer.analyze(projectDir, javaFiles);
    }

    public File exportJson(ArchitectureGraph graph, File targetFile) throws IOException {
        return jsonExporter.export(graph, targetFile);
    }

    public File exportHtml(ArchitectureGraph graph, File targetFile) throws IOException {
        return htmlRenderer.export(graph, targetFile);
    }

    public String formatSummary(ArchitectureGraph graph) {
        return summaryRenderer.render(graph);
    }

    /**
     * Resolves the port dynamically:
     * 1. explicit configPort if provided (> 0)
     * 2. sprout.properties in project root or src/main/resources
     * 3. default 8383
     */
    public int resolvePort(File projectDir, Integer configPort) {
        if (configPort != null && configPort > 0) {
            return configPort;
        }

        if (projectDir != null && projectDir.exists()) {
            File[] propertyLocations = new File[]{
                    new File(projectDir, "sprout.properties"),
                    new File(projectDir, "src/main/resources/sprout.properties")
            };

            for (File propFile : propertyLocations) {
                if (propFile.exists() && propFile.isFile()) {
                    try (FileInputStream fis = new FileInputStream(propFile)) {
                        Properties props = new Properties();
                        props.load(fis);
                        String portVal = props.getProperty("sprout.port", props.getProperty("port"));
                        if (portVal != null && !portVal.trim().isEmpty()) {
                            return Integer.parseInt(portVal.trim());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return DEFAULT_PORT;
    }
}
