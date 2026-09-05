package io.arschaos.web;

import io.arschaos.model.ArchitectureGraph;
import io.arschaos.renderer.HtmlDashboardRenderer;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.Map;

public class SproutWebServer {

    private static final Logger logger = LoggerFactory.getLogger(SproutWebServer.class);

    private Javalin app;
    private int port;
    private final HtmlDashboardRenderer htmlRenderer = new HtmlDashboardRenderer();

    public synchronized int start(int port, ArchitectureGraph graph) {
        if (app != null) {
            return this.port;
        }

        try {
            String htmlContent = htmlRenderer.renderHtml(graph);

            app = Javalin.create(config -> {
                config.showJavalinBanner = false;
            });

            app.get("/", ctx -> {
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(htmlContent);
            });

            app.get("/dashboard.css", ctx -> {
                ctx.contentType("text/css; charset=UTF-8");
                ctx.result(htmlRenderer.getCssContent());
            });

            app.get("/dashboard.js", ctx -> {
                ctx.contentType("application/javascript; charset=UTF-8");
                ctx.result(htmlRenderer.getJsContent());
            });

            app.get("/api/graph", ctx -> ctx.json(graph));
            app.get("/api/metrics", ctx -> ctx.json(graph.getMetrics()));
            app.get("/api/health", ctx -> ctx.json(Map.of("status", "UP")));

            app.start(port);
            this.port = app.port();
            logger.info("Sprout Web Visualizer started at http://localhost:{}/", this.port);
            return this.port;
        } catch (Exception e) {
            logger.error("Failed to start Sprout web visualizer on port {}: {}", port, e.getMessage(), e);
            throw new RuntimeException("Could not start visualizer server on port " + port, e);
        }
    }

    public synchronized void stop() {
        if (app != null) {
            try {
                app.stop();
            } catch (Exception e) {
                logger.warn("Error stopping Javalin server: {}", e.getMessage());
            } finally {
                app = null;
            }
        }
    }

    public synchronized boolean isRunning() {
        return app != null;
    }

    public int getPort() {
        return port;
    }

    /**
     * Attempts to open the system default browser to the visualizer URL.
     * Fails silently/gracefully in headless or unsupported environments.
     */
    public static boolean openBrowser(int port) {
        String url = "http://localhost:" + port + "/";
        try {
            if (!java.awt.GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(url));
                    return true;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not automatically open browser: {}", e.getMessage());
        }
        return false;
    }
}
