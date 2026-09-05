package io.arschaos.renderer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.arschaos.model.ArchitectureGraph;

import java.io.File;
import java.io.IOException;

public class JsonExporter {

    private final ObjectMapper objectMapper;

    public JsonExporter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public String toJson(ArchitectureGraph graph) throws IOException {
        return objectMapper.writeValueAsString(graph);
    }

    public File export(ArchitectureGraph graph, File targetFile) throws IOException {
        if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        objectMapper.writeValue(targetFile, graph);
        return targetFile;
    }
}
