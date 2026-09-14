package io.arschaos.model;

/**
 * Architectural classification for Java and Spring components.
 */
public enum ComponentType {
    CONTROLLER,
    SERVICE,
    REPOSITORY,
    CONFIGURATION,
    COMPONENT,
    MODEL,
    UTILITY,
    INTERFACE,
    RECORD,
    ENUM,
    CLASS;

    public String getDisplayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
