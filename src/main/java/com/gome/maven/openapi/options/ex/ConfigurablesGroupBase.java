/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.openapi.options.ex;

import com.gome.maven.openapi.components.ComponentManager;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ConfigurableEP;
import com.gome.maven.openapi.options.ConfigurableGroup;

import java.util.List;

/**
 * @author nik
 */
public abstract class ConfigurablesGroupBase implements ConfigurableGroup {
    private Configurable[] myChildren;
    private final ComponentManager myComponentManager;
    private final ExtensionPointName<ConfigurableEP<Configurable>> myConfigurablesExtensionPoint;
    private final boolean myLoadComponents;

    protected ConfigurablesGroupBase(ComponentManager componentManager, final ExtensionPointName<ConfigurableEP<Configurable>> configurablesExtensionPoint,
                                     boolean loadComponents) {
        myComponentManager = componentManager;
        myConfigurablesExtensionPoint = configurablesExtensionPoint;
        myLoadComponents = loadComponents;
    }

    @Override
    public Configurable[] getConfigurables() {
        if (myChildren == null) {
            final ConfigurableEP<Configurable>[] extensions = myComponentManager.getExtensions(myConfigurablesExtensionPoint);
            Configurable[] components = myLoadComponents ? myComponentManager.getComponents(Configurable.class) : new Configurable[0];

            List<Configurable> result = ConfigurableExtensionPointUtil.buildConfigurablesList(extensions, components, getConfigurableFilter());
            myChildren = result.toArray(new Configurable[result.size()]);
        }
        return myChildren;
    }

    protected abstract ConfigurableFilter getConfigurableFilter();

    @Override
    public String getShortName() {
        return null;
    }
}
