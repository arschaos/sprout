package io.arschaos.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProjectMetrics {
    private int totalFiles;
    private int totalClasses;
    private int totalInterfaces;
    private int totalLinesOfCode;
    private int packageCount;
    private Map<String, Integer> typeCounts = new LinkedHashMap<>();
    private Map<String, Integer> relationshipCounts = new LinkedHashMap<>();

    public ProjectMetrics() {
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
    }

    public int getTotalInterfaces() {
        return totalInterfaces;
    }

    public void setTotalInterfaces(int totalInterfaces) {
        this.totalInterfaces = totalInterfaces;
    }

    public int getTotalLinesOfCode() {
        return totalLinesOfCode;
    }

    public void setTotalLinesOfCode(int totalLinesOfCode) {
        this.totalLinesOfCode = totalLinesOfCode;
    }

    public int getPackageCount() {
        return packageCount;
    }

    public void setPackageCount(int packageCount) {
        this.packageCount = packageCount;
    }

    public Map<String, Integer> getTypeCounts() {
        return typeCounts;
    }

    public void setTypeCounts(Map<String, Integer> typeCounts) {
        this.typeCounts = typeCounts;
    }

    public Map<String, Integer> getRelationshipCounts() {
        return relationshipCounts;
    }

    public void setRelationshipCounts(Map<String, Integer> relationshipCounts) {
        this.relationshipCounts = relationshipCounts;
    }
}
