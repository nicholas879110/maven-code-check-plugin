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

package com.gome.maven.openapi.vcs.impl;

import com.gome.maven.lifecycle.PeriodicalTasksCloser;
import com.gome.maven.openapi.components.BaseComponent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.AbstractExtensionPointBean;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.AbstractVcs;
import com.gome.maven.openapi.vcs.VcsActiveEnvironmentsProxy;
import com.gome.maven.util.xmlb.annotations.Attribute;

/**
 * @author yole
 */
public class VcsEP extends AbstractExtensionPointBean {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vcs.impl.VcsEP");

    public static final ExtensionPointName<VcsEP> EP_NAME = ExtensionPointName.create("com.gome.maven.vcs");

    // these must be public for scrambling compatibility
    @Attribute("name")
    public String name;
    @Attribute("vcsClass")
    public String vcsClass;
    @Attribute("displayName")
    public String displayName;
    @Attribute("administrativeAreaName")
    public String administrativeAreaName;
    @Attribute("crawlUpToCheckUnderVcs")
    public boolean crawlUpToCheckUnderVcs;

    private AbstractVcs myVcs;
    private final Object LOCK = new Object();

    
    public AbstractVcs getVcs( Project project) {
        synchronized (LOCK) {
            if (myVcs != null) {
                return myVcs;
            }
        }
        AbstractVcs vcs = getInstance(project, vcsClass);
        synchronized (LOCK) {
            if (myVcs == null) {
                myVcs = VcsActiveEnvironmentsProxy.proxyVcs(vcs);
            }
            return myVcs;
        }
    }

    
    private AbstractVcs getInstance( Project project,  String vcsClass) {
        try {
            final Class<? extends AbstractVcs> foundClass = findClass(vcsClass);
            final Class<?>[] interfaces = foundClass.getInterfaces();
            for (Class<?> anInterface : interfaces) {
                if (BaseComponent.class.isAssignableFrom(anInterface)) {
                    return PeriodicalTasksCloser.getInstance().safeGetComponent(project, foundClass);
                }
            }
            return instantiate(vcsClass, project.getPicoContainer());
        }
        catch(Exception e) {
            LOG.error(e);
            return null;
        }
    }

    
    public VcsDescriptor createDescriptor() {
        return new VcsDescriptor(administrativeAreaName, displayName, name, crawlUpToCheckUnderVcs);
    }
}
