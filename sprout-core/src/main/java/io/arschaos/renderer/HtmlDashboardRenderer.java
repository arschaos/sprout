package io.arschaos.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.arschaos.model.ArchitectureGraph;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class HtmlDashboardRenderer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String htmlTemplate;
    private final String cssContent;
    private final String jsContent;

    public HtmlDashboardRenderer() {
        this.htmlTemplate = loadResource("/sprout/dashboard.html");
        this.cssContent = loadResource("/sprout/dashboard.css");
        this.jsContent = loadResource("/sprout/dashboard.js");
    }

    public String getCssContent() {
        return cssContent;
    }

    public String getJsContent() {
        return jsContent;
    }

    public String renderHtml(ArchitectureGraph graph) throws IOException {
        String jsonGraph = objectMapper.writeValueAsString(graph);
        String projectName = escapeHtml(graph.getProjectName() != null ? graph.getProjectName() : "Project");
        String timestamp = escapeHtml(graph.getTimestamp() != null ? graph.getTimestamp() : "");

        return htmlTemplate
                .replace("/*{{PROJECT_NAME}}*/", projectName)
                .replace("/*{{TIMESTAMP}}*/", timestamp)
                .replace("/*{{SPROUT_DATA}}*/null", jsonGraph);
    }

    /**
     * Exports the dashboard files into the target directory:
     * - The HTML file (e.g. index.html)
     * - dashboard.css
     * - dashboard.js
     */
    public File export(ArchitectureGraph graph, File targetHtmlFile) throws IOException {
        File parentDir = targetHtmlFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 1. Export HTML
        String renderedHtml = renderHtml(graph);
        Files.writeString(targetHtmlFile.toPath(), renderedHtml, StandardCharsets.UTF_8);

        // 2. Export CSS
        if (parentDir != null) {
            File cssFile = new File(parentDir, "dashboard.css");
            Files.writeString(cssFile.toPath(), cssContent, StandardCharsets.UTF_8);

            // 3. Export JS
            File jsFile = new File(parentDir, "dashboard.js");
            Files.writeString(jsFile.toPath(), jsContent, StandardCharsets.UTF_8);
        }

        return targetHtmlFile;
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Resource not found on classpath: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not load classpath resource: " + path, e);
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
