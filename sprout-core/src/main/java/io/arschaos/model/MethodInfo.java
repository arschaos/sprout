package io.arschaos.model;

import java.util.ArrayList;
import java.util.List;

public class MethodInfo {
    private String name;
    private String returnType;
    private List<String> parameterTypes = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();

    public MethodInfo() {
    }

    public MethodInfo(String name, String returnType, List<String> parameterTypes, List<String> annotations) {
        this.name = name;
        this.returnType = returnType;
        if (parameterTypes != null) {
            this.parameterTypes = parameterTypes;
        }
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

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(List<String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }
}
