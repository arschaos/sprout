package io.arschaos.web;

import io.arschaos.model.ArchitectureGraph;
import io.arschaos.model.ComponentNode;
import io.arschaos.model.ComponentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SproutWebServerTest {

    private SproutWebServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void shouldStartAndServeEndpoints() throws Exception {
        ArchitectureGraph graph = new ArchitectureGraph();
        graph.setProjectName("test-proj");
        graph.getNodes().add(new ComponentNode("com.test.App", "App", "com.test", ComponentType.CLASS));

        server = new SproutWebServer();
        int port = server.start(0, graph);

        assertThat(port).isGreaterThan(0);
        assertThat(server.isRunning()).isTrue();

        HttpClient client = HttpClient.newHttpClient();

        // 1. GET /
        HttpResponse<String> indexRes = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(indexRes.statusCode()).isEqualTo(200);
        assertThat(indexRes.body()).contains("Sprout Architecture Visualizer", "test-proj");

        // 2. GET /api/health
        HttpResponse<String> healthRes = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/api/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(healthRes.statusCode()).isEqualTo(200);
        assertThat(healthRes.body()).contains("UP");

        // 3. GET /api/graph
        HttpResponse<String> graphRes = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/api/graph")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(graphRes.statusCode()).isEqualTo(200);
        assertThat(graphRes.body()).contains("test-proj", "com.test.App");

        // 4. GET /dashboard.css
        HttpResponse<String> cssRes = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/dashboard.css")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(cssRes.statusCode()).isEqualTo(200);
        assertThat(cssRes.headers().firstValue("Content-Type")).hasValueSatisfying(ct -> assertThat(ct).contains("text/css"));
        assertThat(cssRes.body()).contains("--bg-color", "canvas");

        // 5. GET /dashboard.js
        HttpResponse<String> jsRes = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/dashboard.js")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(jsRes.statusCode()).isEqualTo(200);
        assertThat(jsRes.headers().firstValue("Content-Type")).hasValueSatisfying(ct -> assertThat(ct).contains("javascript"));
        assertThat(jsRes.body()).contains("initDashboard", "COLOR_MAP");

        server.stop();
        assertThat(server.isRunning()).isFalse();
    }
}
