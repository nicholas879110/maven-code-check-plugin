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
package com.gome.maven.openapi.ui;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;

import java.awt.*;

public abstract class DialogWrapperPeerFactory {
    
    public static DialogWrapperPeerFactory getInstance() {
        if (ApplicationManager.getApplication() == null) {
            return getInstanceByName();
        }

        DialogWrapperPeerFactory factory = ServiceManager.getService(DialogWrapperPeerFactory.class);
        return factory == null ? getInstanceByName() : factory;
    }

    
    private static DialogWrapperPeerFactory getInstanceByName() {
        try {
            return (DialogWrapperPeerFactory)Class.forName("com.gome.maven.openapi.ui.impl.DialogWrapperPeerFactoryImpl").newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException("Can't instantiate DialogWrapperPeerFactory", e);
        }
    }

    
    public abstract DialogWrapperPeer createPeer( DialogWrapper wrapper,  Project project, boolean canBeParent);
    
    public abstract DialogWrapperPeer createPeer( DialogWrapper wrapper, boolean canBeParent);

    
    public abstract DialogWrapperPeer createPeer( DialogWrapper wrapper,  Project project, boolean canBeParent, DialogWrapper.IdeModalityType ideModalityType);

    /** @see DialogWrapper#DialogWrapper(boolean, boolean)
     */
    @Deprecated
    
    public abstract DialogWrapperPeer createPeer( DialogWrapper wrapper, boolean canBeParent, boolean applicationModalIfPossible);
    @Deprecated
    
    public abstract DialogWrapperPeer createPeer( DialogWrapper wrapper, Window owner, boolean canBeParent, boolean applicationModalIfPossible);
    @Deprecated
    
    public abstract DialogWrapperPeer createPeer( DialogWrapper wrapper,  Component parent, boolean canBeParent);

    
    public abstract DialogWrapperPeer createPeer( DialogWrapper wrapper, boolean canBeParent, DialogWrapper.IdeModalityType ideModalityType);
    
    public abstract DialogWrapperPeer createPeer( DialogWrapper wrapper, Window owner, boolean canBeParent, DialogWrapper.IdeModalityType ideModalityType);
}