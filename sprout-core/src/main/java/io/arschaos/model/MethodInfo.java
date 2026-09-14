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
public class MethodInfo {
    private String name;
    private String returnType;
    private List<String> parameterTypes = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();

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
}
