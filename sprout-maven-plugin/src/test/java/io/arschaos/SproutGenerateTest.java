package io.arschaos;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SproutGenerateTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExecuteMojoSuccessfully() throws IOException, MojoExecutionException {
        Path src = tempDir.resolve("src/main/java/com/test");
        Files.createDirectories(src);

        Files.writeString(src.resolve("AppService.java"),
            "package com.test;\n" +
            "import org.springframework.stereotype.Service;\n" +
            "@Service\n" +
            "public class AppService {}\n");

        Files.writeString(src.resolve("AppController.java"),
            "package com.test;\n" +
            "import org.springframework.web.bind.annotation.RestController;\n" +
            "import lombok.RequiredArgsConstructor;\n" +
            "@RestController\n" +
            "@RequiredArgsConstructor\n" +
            "public class AppController {\n" +
            "    private final AppService service;\n" +
            "}\n");

        File outDir = tempDir.resolve("target/sprout").toFile();

        SproutGenerate mojo = new SproutGenerate();
        mojo.testBaseDir = tempDir.toFile();
        mojo.outputDirectory = tempDir.resolve("target/sprout").toFile();
        mojo.formats = List.of("HTML", "JSON", "MERMAID");
        mojo.launchApp = false;

        mojo.execute();

        File json = new File(outDir, "architecture.json");
        File html = new File(outDir, "index.html");
        File css = new File(outDir, "dashboard.css");
        File js = new File(outDir, "dashboard.js");

        assertThat(json).exists();
        assertThat(html).exists();
        assertThat(css).exists();
        assertThat(js).exists();

        String jsonText = Files.readString(json.toPath(), StandardCharsets.UTF_8);
        assertThat(jsonText).contains("AppController", "AppService", "INJECTS");
        String htmlText = Files.readString(html.toPath(), StandardCharsets.UTF_8);
        assertThat(htmlText).contains("dashboard.css", "dashboard.js");
    }
}
