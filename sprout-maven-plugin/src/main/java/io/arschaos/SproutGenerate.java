package io.arschaos;

import io.arschaos.core.SproutEngine;
import io.arschaos.model.ArchitectureGraph;
import io.arschaos.web.SproutWebServer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Sprout Maven Mojo that scans the target Java codebase, extracts architectural relationships
 * (extends, implements, dependency injection, fields, and method calls), serializes the result
 * into a JSON diagram file, and optionally launches a local interactive visualization server.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class SproutGenerate extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;

    /**
     * Control flag to launch the embedded Javalin server and automatically open the interactive 
     * dashboard in the user's default browser after scanning.
     */
    @Parameter(defaultValue = "true", property = "sprout.launchApp")
    private boolean launchApp;

    /**
     * Port configuration option for the visualizer web server. If not specified, 
     * Sprout resolves the port dynamically from local configuration files (e.g. {@code sprout.properties})
     * or defaults to {@code 8383}.
     */
    @Parameter(property = "sprout.port")
    private Integer port;

    /**
     * Directory where the generated architecture artifacts (JSON and HTML dashboard) are stored.
     */
    @Parameter(defaultValue = "${project.build.directory}/sprout", property = "sprout.outputDir")
    private File outputDir;

    /** Visible for testing. */
    File testBaseDir;

    /**
     * Executes the Mojo goal. Validates inputs, scans and analyzes the target directory via
     * {@link SproutEngine}, outputs the serialized JSON graph, and optionally hosts and launches
     * the web visualizer application.
     * 
     * @throws MojoExecutionException If an unrecoverable validation, file write, or server error occurs.
     */
    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Sprout is generating architecture diagrams...");

        File targetProjectDir = testBaseDir != null ? testBaseDir :
                (baseDir != null ? baseDir :
                (project != null && project.getBasedir() != null ? project.getBasedir() : new File(".")));

        if (!targetProjectDir.exists()) {
            throw new MojoExecutionException("Target project directory does not exist: " + targetProjectDir.getAbsolutePath());
        }

        try {
            SproutEngine engine = new SproutEngine();

            // 1. Scan and analyze
            ArchitectureGraph graph = engine.analyze(targetProjectDir);

            // 2. Prepare output directory
            File outDirectory = outputDir != null ? outputDir : new File(targetProjectDir, "target/sprout");
            if (!outDirectory.exists()) {
                outDirectory.mkdirs();
            }

            // 3. Export JSON and HTML
            File jsonFile = new File(outDirectory, "architecture.json");
            engine.exportJson(graph, jsonFile);

            File htmlFile = new File(outDirectory, "index.html");
            engine.exportHtml(graph, htmlFile);

            // 4. Log summary
            String summary = engine.formatSummary(graph);
            getLog().info(summary);

            getLog().info("Architecture artifacts generated:");
            getLog().info("  JSON Graph : " + jsonFile.getAbsolutePath());
            getLog().info("  HTML Report: " + htmlFile.getAbsolutePath());

            // 5. Optional interactive visualizer server
            if (launchApp) {
                int serverPort = engine.resolvePort(targetProjectDir, port);
                SproutWebServer webServer = new SproutWebServer();
                webServer.start(serverPort, graph);
                getLog().info("Sprout Web Visualizer is live at: http://localhost:" + serverPort + "/");

                SproutWebServer.openBrowser(serverPort);

                if (System.console() != null) {
                    getLog().info("Press [ENTER] in terminal to shut down visualizer...");
                    try {
                        System.in.read();
                    } catch (Exception ignored) {
                    } finally {
                        webServer.stop();
                        getLog().info("Sprout Web Visualizer stopped.");
                    }
                } else if (Boolean.getBoolean("sprout.keepAlive")) {
                    getLog().info("Visualizer server running in background daemon mode.");
                } else {
                    // Headless / non-interactive / CI execution
                    webServer.stop();
                }
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Failed during Sprout architecture generation: " + e.getMessage(), e);
        }
    }

    public void setTestBaseDir(File testBaseDir) {
        this.testBaseDir = testBaseDir;
    }

    public void setLaunchApp(boolean launchApp) {
        this.launchApp = launchApp;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }
}
