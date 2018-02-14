package com.gome.maven.openapi.components;


public interface NamedComponent {
    /**
     * Unique name of this component. If there is another component with the same name or
     * name is null internal assertion will occur.
     *
     * @return the name of this component
     */
    String getComponentName();
}
