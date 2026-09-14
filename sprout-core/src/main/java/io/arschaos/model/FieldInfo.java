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
public class FieldInfo {
    private String name;
    private String type;
    private boolean isFinal;
    private List<String> annotations = new ArrayList<>();

    public FieldInfo(String name, String type, boolean isFinal, List<String> annotations) {
        this.name = name;
        this.type = type;
        this.isFinal = isFinal;
        if (annotations != null) {
            this.annotations = annotations;
        }
    }
}
