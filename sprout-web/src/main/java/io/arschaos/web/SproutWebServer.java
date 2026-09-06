package io.arschaos.web;

import io.arschaos.model.ArchitectureGraph;
import io.arschaos.renderer.HtmlDashboardRenderer;
import io.javalin.Javalin;
import io.javalin.util.JavalinLogger;
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

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(SproutWebServer.class.getClassLoader());
            JavalinLogger.startupInfo = false;
            JavalinLogger.enabled = false;

            String htmlContent = htmlRenderer.renderHtml(graph);

            int boundPort = port;
            boolean started = false;
            for (int attempt = 0; attempt < 20; attempt++) {
                if (boundPort > 0 && !isPortAvailable(boundPort)) {
                    logger.warn("Port {} is in use, trying {}...", boundPort, boundPort + 1);
                    boundPort++;
                    continue;
                }
                app = createApp(graph, htmlContent);
                try {
                    app.start(boundPort);
                    this.port = app.port();
                    started = true;
                    break;
                } catch (Exception e) {
                    try {
                        app.stop();
                    } catch (Exception ignored) {
                    }
                    app = null;

                    if (isAddressInUse(e) && boundPort > 0) {
                        logger.warn("Port {} is in use, trying {}...", boundPort, boundPort + 1);
                        boundPort++;
                        continue;
                    }
                    throw e;
                }
            }

            if (!started) {
                throw new RuntimeException("Could not find an available port starting from " + port);
            }

            logger.info("Sprout Web Visualizer started at http://localhost:{}/", this.port);
            
            return this.port;
        } catch (Exception e) {
            logger.error("Failed to start Sprout web visualizer on port {}: {}", port, e.getMessage(), e);
            throw new RuntimeException("Could not start visualizer server on port " + port, e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private Javalin createApp(ArchitectureGraph graph, String htmlContent) {
        Javalin newApp = Javalin.create(config -> {
            config.showJavalinBanner = false;
        });

        newApp.get("/", ctx -> {
            ctx.contentType("text/html; charset=UTF-8");
            ctx.result(htmlContent);
        });

        newApp.get("/dashboard.css", ctx -> {
            ctx.contentType("text/css; charset=UTF-8");
            ctx.result(htmlRenderer.getCssContent());
        });

        newApp.get("/dashboard.js", ctx -> {
            ctx.contentType("application/javascript; charset=UTF-8");
            ctx.result(htmlRenderer.getJsContent());
        });

        newApp.get("/api/graph", ctx -> ctx.json(graph));
        newApp.get("/api/metrics", ctx -> ctx.json(graph.getMetrics()));
        newApp.get("/api/health", ctx -> ctx.json(Map.of("status", "UP")));

        return newApp;
    }

    private boolean isPortAvailable(int port) {
        if (port <= 0) {
            return true;
        }
        try (java.net.ServerSocket ss = new java.net.ServerSocket()) {
            ss.setReuseAddress(true);
            ss.bind(new java.net.InetSocketAddress(port));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAddressInUse(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof java.net.BindException
                    || (cause.getMessage() != null && cause.getMessage().contains("Address already in use"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public synchronized void stop() {
        if (app != null) {
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(SproutWebServer.class.getClassLoader());
                app.stop();
            } catch (Exception e) {
                logger.warn("Error stopping Javalin server: {}", e.getMessage());
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
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
            if (System.getProperty("os.name", "").toLowerCase().contains("linux")) {
                new ProcessBuilder("xdg-open", url).start();
                return true;
            }
        } catch (Exception e) {
            logger.debug("Could not automatically open browser: {}", e.getMessage());
        }
        return false;
    }
}
