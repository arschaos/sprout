package io.arschaos.model;

import java.util.Objects;

public class ComponentEdge {
    private String id;
    private String source;
    private String target;
    private RelationshipType type;
    private String description;

    public ComponentEdge() {
    }

    public ComponentEdge(String source, String target, RelationshipType type, String description) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.description = description;
        this.id = source + "->" + target + ":" + type + (description != null ? ":" + description : "");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public RelationshipType getType() {
        return type;
    }

    public void setType(RelationshipType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentEdge that = (ComponentEdge) o;
        return Objects.equals(source, that.source) &&
               Objects.equals(target, that.target) &&
               type == that.type &&
               Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, type, description);
    }
}
