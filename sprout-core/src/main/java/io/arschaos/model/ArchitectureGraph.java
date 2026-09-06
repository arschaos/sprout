package io.arschaos.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ArchitectureGraph {
    private String projectName;
    private String timestamp;
    private String baseDirectory;
    private ProjectMetrics metrics = new ProjectMetrics();
    private List<ComponentNode> nodes = new ArrayList<>();
    private List<ComponentEdge> edges = new ArrayList<>();
}
