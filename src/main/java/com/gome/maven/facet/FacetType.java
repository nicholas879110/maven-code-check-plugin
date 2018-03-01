/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gome.maven.facet;

import com.gome.maven.facet.autodetecting.FacetDetectorRegistry;
import com.gome.maven.facet.ui.DefaultFacetSettingsEditor;
import com.gome.maven.facet.ui.FacetEditor;
import com.gome.maven.facet.ui.MultipleFacetSettingsEditor;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleType;
import com.gome.maven.openapi.project.Project;

import javax.swing.*;

/**
 * Override this class to provide custom type of facets. The implementation should be registered in your {@code plugin.xml}:
 * <pre>
 * &lt;extensions defaultExtensionNs="com.gome.maven"&gt;
 * &nbsp;&nbsp;&lt;facetType implementation="qualified-class-name"/&gt;
 * &lt;/extensions&gt;
 * </pre>
 * @author nik
 */
public abstract class FacetType<F extends Facet, C extends FacetConfiguration> {
    public static final ExtensionPointName<FacetType> EP_NAME = ExtensionPointName.create("com.gome.maven.facetType");

    private final  FacetTypeId<F> myId;
    private final  String myStringId;
    private final  String myPresentableName;
    private final  FacetTypeId myUnderlyingFacetType;

    public static <T extends FacetType> T findInstance(Class<T> aClass) {
        return EP_NAME.findExtension(aClass);
    }

    /**
     * @param id unique instance of {@link FacetTypeId}
     * @param stringId unique string id of the facet type
     * @param presentableName name of this facet type which will be shown in UI
     * @param underlyingFacetType if this parameter is not <code>null</code> then you will be able to add facets of this type only as
     * subfacets to a facet of the specified type. If this parameter is <code>null</code> it will be possible to add facet of this type
     * directly to a module
     */
    public FacetType(final  FacetTypeId<F> id, final   String stringId, final  String presentableName,
                     final  FacetTypeId underlyingFacetType) {
        myId = id;
        myStringId = stringId;
        myPresentableName = presentableName;
        myUnderlyingFacetType = underlyingFacetType;
    }


    /**
     * @param id unique instance of {@link FacetTypeId}
     * @param stringId unique string id of the facet type
     * @param presentableName name of this facet type which will be shown in UI
     */
    public FacetType(final  FacetTypeId<F> id, final   String stringId, final  String presentableName) {
        this(id, stringId, presentableName, null);
    }

    
    public final FacetTypeId<F> getId() {
        return myId;
    }

    
    public final String getStringId() {
        return myStringId;
    }

    
    public String getPresentableName() {
        return myPresentableName;
    }

    /**
     * Default name which will be used then user creates a facet of this type
     * @return
     */
     
    public String getDefaultFacetName() {
        return myPresentableName;
    }

    
    public final FacetTypeId<?> getUnderlyingFacetType() {
        return myUnderlyingFacetType;
    }

    /**
     * @deprecated this method is not called by IDEA core anymore. Use {@link com.gome.maven.framework.detection.FrameworkDetector} extension
     * to provide automatic detection for facets
     */
    public void registerDetectors(FacetDetectorRegistry<C> registry) {
    }

    /**
     * Create default configuration of facet. See {@link FacetConfiguration} for details
     * @return
     */
    public abstract C createDefaultConfiguration();

    /**
     * Create a new facet instance
     * @param module parent module for facet. Must be passed to {@link Facet} constructor
     * @param name name of facet. Must be passed to {@link Facet} constructor
     * @param configuration facet configuration. Must be passed to {@link Facet} constructor
     * @param underlyingFacet underlying facet. Must be passed to {@link Facet} constructor
     * @return a created facet
     */
    public abstract F createFacet( Module module, final String name,  C configuration,  Facet underlyingFacet);

    /**
     * @return <code>true</code> if only one facet of this type is allowed within the containing module (if this type doesn't have the underlying
     * facet type) or within the underlying facet
     */
    public boolean isOnlyOneFacetAllowed() {
        return true;
    }

    /**
     * @param moduleType type of module
     * @return <code>true</code> if facet of this type are allowed in module of type <code>moduleType</code>
     */
    public abstract boolean isSuitableModuleType(ModuleType moduleType);

    
    public Icon getIcon() {
        return null;
    }

    /**
     * Returns the topic in the help file which is shown when help for this facet type is requested
     *
     * @return the help topic, or null if no help is available.
     */
     
    public String getHelpTopic() {
        return null;
    }

    
    public DefaultFacetSettingsEditor createDefaultConfigurationEditor( Project project,  C configuration) {
        return null;
    }

    /**
     * Override to allow editing several facets at once
     * @param project project
     * @param editors editors of selected facets
     * @return editor
     */
    
    public MultipleFacetSettingsEditor createMultipleConfigurationsEditor( Project project,  FacetEditor[] editors) {
        return null;
    }
}
