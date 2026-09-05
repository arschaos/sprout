package io.arschaos.model;

import java.util.ArrayList;
import java.util.List;

public class ComponentNode {
    private String id;
    private String name;
    private String packageName;
    private ComponentType type;
    private List<String> annotations = new ArrayList<>();
    private List<FieldInfo> fields = new ArrayList<>();
    private List<MethodInfo> methods = new ArrayList<>();
    private int linesOfCode;
    private String sourceFile;
    private String superClass;
    private List<String> interfaces = new ArrayList<>();

    public ComponentNode() {
    }

    public ComponentNode(String id, String name, String packageName, ComponentType type) {
        this.id = id;
        this.name = name;
        this.packageName = packageName;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public ComponentType getType() {
        return type;
    }

    public void setType(ComponentType type) {
        this.type = type;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public void setFields(List<FieldInfo> fields) {
        this.fields = fields;
    }

    public List<MethodInfo> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodInfo> methods) {
        this.methods = methods;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getSuperClass() {
        return superClass;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
    }
}
