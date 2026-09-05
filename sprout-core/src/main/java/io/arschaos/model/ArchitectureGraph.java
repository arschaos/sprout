package io.arschaos.model;

import java.util.ArrayList;
import java.util.List;

public class ArchitectureGraph {
    private String projectName;
    private String timestamp;
    private String baseDirectory;
    private ProjectMetrics metrics = new ProjectMetrics();
    private List<ComponentNode> nodes = new ArrayList<>();
    private List<ComponentEdge> edges = new ArrayList<>();

    public ArchitectureGraph() {
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public ProjectMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(ProjectMetrics metrics) {
        this.metrics = metrics;
    }

    public List<ComponentNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<ComponentNode> nodes) {
        this.nodes = nodes;
    }

    public List<ComponentEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<ComponentEdge> edges) {
        this.edges = edges;
    }
}
