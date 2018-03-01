/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.changes.ui;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.PluginAware;
import com.gome.maven.openapi.extensions.PluginDescriptor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.NotNullFunction;
import com.gome.maven.util.pico.ConstructorInjectionComponentAdapter;
import com.gome.maven.util.xmlb.annotations.Attribute;

/**
 * @author yole
 */
public class ChangesViewContentEP implements PluginAware {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vcs.changes.ui.ChangesViewContentEP");

    public static final ExtensionPointName<ChangesViewContentEP> EP_NAME = new ExtensionPointName<ChangesViewContentEP>("com.gome.maven.changesViewContent");

    @Attribute("tabName")
    public String tabName;

    @Attribute("className")
    public String className;

    @Attribute("predicateClassName")
    public String predicateClassName;

    private PluginDescriptor myPluginDescriptor;
    private ChangesViewContentProvider myInstance;

    public void setPluginDescriptor(PluginDescriptor pluginDescriptor) {
        myPluginDescriptor = pluginDescriptor;
    }

    public String getTabName() {
        return tabName;
    }

    public void setTabName(final String tabName) {
        this.tabName = tabName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(final String className) {
        this.className = className;
    }

    public String getPredicateClassName() {
        return predicateClassName;
    }

    public void setPredicateClassName(final String predicateClassName) {
        this.predicateClassName = predicateClassName;
    }

    public ChangesViewContentProvider getInstance(Project project) {
        if (myInstance == null) {
            myInstance = (ChangesViewContentProvider) newClassInstance(project, className);
        }
        return myInstance;
    }


    public NotNullFunction<Project, Boolean> newPredicateInstance(Project project) {
        //noinspection unchecked
        return predicateClassName != null ? (NotNullFunction<Project, Boolean>)newClassInstance(project, predicateClassName) : null;
    }

    private Object newClassInstance(final Project project, final String className) {
        try {
            final Class<?> aClass = Class.forName(className, true,
                    myPluginDescriptor == null ? getClass().getClassLoader()  : myPluginDescriptor.getPluginClassLoader());
            return new ConstructorInjectionComponentAdapter(className, aClass).getComponentInstance(project.getPicoContainer());
        }
        catch(Exception e) {
            LOG.error(e);
            return null;
        }
    }
}
