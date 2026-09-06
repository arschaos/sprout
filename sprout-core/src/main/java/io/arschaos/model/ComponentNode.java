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

    public ComponentNode(String id, String name, String packageName, ComponentType type) {
        this.id = id;
        this.name = name;
        this.packageName = packageName;
        this.type = type;
    }
}
