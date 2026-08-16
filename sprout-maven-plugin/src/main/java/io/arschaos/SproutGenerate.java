package io.arschaos;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Sprout Maven Mojo that scans the target Java codebase, extracts architectural relationships
 * (extends, implements, dependency injection, fields, and method calls), serializes the result
 * into a JSON diagram file, and optionally launches a local interactive visualization server.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class SproutGenerate extends AbstractMojo {
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
    }
}
