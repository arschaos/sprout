package io.arschaos.model;

import java.util.ArrayList;
import java.util.List;

public class FieldInfo {
    private String name;
    private String type;
    private boolean isFinal;
    private List<String> annotations = new ArrayList<>();

    public FieldInfo() {
    }

    public FieldInfo(String name, String type, boolean isFinal, List<String> annotations) {
        this.name = name;
        this.type = type;
        this.isFinal = isFinal;
        if (annotations != null) {
            this.annotations = annotations;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }
}
