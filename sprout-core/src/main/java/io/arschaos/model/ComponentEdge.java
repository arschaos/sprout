package io.arschaos.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(exclude = "id")
public class ComponentEdge {
    private String id;
    private String source;
    private String target;
    private RelationshipType type;
    private String description;

    public ComponentEdge(String source, String target, RelationshipType type, String description) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.description = description;
        this.id = source + "->" + target + ":" + type + (description != null ? ":" + description : "");
    }
}
