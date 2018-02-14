/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.openapi.updateSettings.impl.pluginsAdvertisement;

import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.project.Project;
import org.jdom.Element;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: anna
 */
@State(
        name = "UnknownFeatures",
        storages = {
                @Storage(
                        file = StoragePathMacros.WORKSPACE_FILE
                )}
)
public class UnknownFeaturesCollector implements PersistentStateComponent<Element> {
     private static final String FEATURE_ID = "featureType";
     private static final String IMPLEMENTATION_NAME = "implementationName";

    private final Set<UnknownFeature> myUnknownFeatures = new HashSet<UnknownFeature>();
    private final Set<UnknownFeature> myIgnoredUnknownFeatures = new HashSet<UnknownFeature>();

    public static UnknownFeaturesCollector getInstance(Project project) {
        return ServiceManager.getService(project, UnknownFeaturesCollector.class);
    }

    public void registerUnknownRunConfiguration(String configurationName) {
        registerUnknownFeature("com.gome.maven.configurationType", configurationName, "Run Configuration");
    }

    public void registerUnknownFeature(String featureType, String implementationName, String featureDisplayName) {
        final UnknownFeature feature = new UnknownFeature(featureType, featureDisplayName, implementationName);
        if (!isIgnored(feature)) {
            myUnknownFeatures.add(feature);
        }
    }

    public boolean isIgnored(UnknownFeature feature) {
        return myIgnoredUnknownFeatures.contains(feature);
    }

    public void ignoreFeature(UnknownFeature feature) {
        myIgnoredUnknownFeatures.add(feature);
    }

    public Set<UnknownFeature> getUnknownFeatures() {
        return myUnknownFeatures;
    }

    
    @Override
    public Element getState() {
        if (myIgnoredUnknownFeatures.isEmpty()) return null;

        final Element ignored = new Element("ignored");
        for (UnknownFeature feature : myIgnoredUnknownFeatures) {
            final Element option = new Element("option");
            option.setAttribute(FEATURE_ID, feature.getFeatureType());
            option.setAttribute(IMPLEMENTATION_NAME, feature.getImplementationName());
            ignored.addContent(option);
        }
        return ignored;
    }

    @Override
    public void loadState(Element state) {
        myIgnoredUnknownFeatures.clear();
        for (Element element :(List<Element>) state.getChildren()) {
            myIgnoredUnknownFeatures.add(
                    new UnknownFeature(element.getAttributeValue(FEATURE_ID), null, element.getAttributeValue(IMPLEMENTATION_NAME)));
        }
    }
}
