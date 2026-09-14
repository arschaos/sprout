package io.arschaos.renderer;

import io.arschaos.model.ArchitectureGraph;
import io.arschaos.model.ComponentEdge;
import io.arschaos.model.ComponentNode;
import io.arschaos.model.ProjectMetrics;

import java.util.Map;

public class SummaryTextRenderer {

    public String render(ArchitectureGraph graph) {
        StringBuilder sb = new StringBuilder();
        ProjectMetrics m = graph.getMetrics();

        sb.append("\n========================================================================\n");
        sb.append("                       SPROUT ARCHITECTURE ANALYSIS                     \n");
        sb.append("========================================================================\n");
        sb.append(String.format(" Project Name        : %s\n", graph.getProjectName()));
        sb.append(String.format(" Scanned Files       : %d\n", m.getTotalFiles()));
        sb.append(String.format(" Total Lines of Code : %d\n", m.getTotalLinesOfCode()));
        sb.append(String.format(" Discovered Packages : %d\n", m.getPackageCount()));
        sb.append(String.format(" Components / Types  : %d classes (%d interfaces)\n", m.getTotalClasses(), m.getTotalInterfaces()));
        sb.append(String.format(" Relationships Found : %d\n", graph.getEdges().size()));
        sb.append("------------------------------------------------------------------------\n");
        sb.append(" Component Breakdown:\n");
        for (Map.Entry<String, Integer> entry : m.getTypeCounts().entrySet()) {
            sb.append(String.format("   - %-16s: %d\n", entry.getKey(), entry.getValue()));
        }
        sb.append("------------------------------------------------------------------------\n");
        sb.append(" Relationship Breakdown:\n");
        for (Map.Entry<String, Integer> entry : m.getRelationshipCounts().entrySet()) {
            sb.append(String.format("   - %-16s: %d\n", entry.getKey(), entry.getValue()));
        }
        sb.append("------------------------------------------------------------------------\n");
        sb.append(" Key Architectural Components:\n");
        for (ComponentNode node : graph.getNodes()) {
            if (node.getType().name().matches("CONTROLLER|SERVICE|REPOSITORY|CONFIGURATION")) {
                sb.append(String.format("   [%-13s] %s (%s)\n", node.getType(), node.getName(), node.getPackageName()));
            }
        }
        sb.append("------------------------------------------------------------------------\n");
        sb.append(" Sample Dependencies & Injections:\n");
        int count = 0;
        for (ComponentEdge edge : graph.getEdges()) {
            if (count++ >= 8) break;
            String srcSimple = edge.getSource().substring(edge.getSource().lastIndexOf('.') + 1);
            String tgtSimple = edge.getTarget().substring(edge.getTarget().lastIndexOf('.') + 1);
            sb.append(String.format("   %s --[%s]--> %s (%s)\n", srcSimple, edge.getType(), tgtSimple, edge.getDescription()));
        }
        sb.append("========================================================================\n");

        return sb.toString();
    }
}
