package io.arschaos.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ProjectMetrics {
    private int totalFiles;
    private int totalClasses;
    private int totalInterfaces;
    private int totalLinesOfCode;
    private int packageCount;
    private Map<String, Integer> typeCounts = new LinkedHashMap<>();
    private Map<String, Integer> relationshipCounts = new LinkedHashMap<>();
}
