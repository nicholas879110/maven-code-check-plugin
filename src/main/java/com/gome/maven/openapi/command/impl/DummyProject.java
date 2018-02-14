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
package com.gome.maven.openapi.command.impl;

import com.gome.maven.openapi.components.BaseComponent;
import com.gome.maven.openapi.components.ComponentConfig;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.messages.MessageBus;
import org.picocontainer.PicoContainer;

/**
 * @author max
 */
public class DummyProject extends UserDataHolderBase implements Project {

    private static class DummyProjectHolder {
        private static final DummyProject ourInstance = new DummyProject();
    }

    
    public static Project getInstance() {
        return DummyProjectHolder.ourInstance;
    }

    private DummyProject() {
    }

    @Override
    public VirtualFile getProjectFile() {
        return null;
    }

    @Override
    
    public String getName() {
        return "";
    }

    @Override
    
    
    public String getPresentableUrl() {
        return null;
    }

    @Override
    
    
    public String getLocationHash() {
        return "dummy";
    }

    @Override
    
    public String getProjectFilePath() {
        return "";
    }

    @Override
    public VirtualFile getWorkspaceFile() {
        return null;
    }

    @Override
    
    public VirtualFile getBaseDir() {
        return null;
    }

    
    @Override
    public String getBasePath() {
        return null;
    }

    @Override
    public void save() {
    }

    @Override
    public BaseComponent getComponent( String name) {
        return null;
    }

    @Override
    public <T> T getComponent( Class<T> interfaceClass) {
        return null;
    }

    @Override
    public boolean hasComponent( Class interfaceClass) {
        return false;
    }

    @Override
    
    public <T> T[] getComponents( Class<T> baseClass) {
        return (T[]) ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Override
    
    public PicoContainer getPicoContainer() {
        throw new UnsupportedOperationException("getPicoContainer is not implement in : " + getClass());
    }

    @Override
    public <T> T getComponent( Class<T> interfaceClass, T defaultImplementation) {
        return null;
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    
    public Condition getDisposed() {
        return new Condition() {
            @Override
            public boolean value(final Object o) {
                return isDisposed();
            }
        };
    }

    
    public ComponentConfig[] getComponentConfigurations() {
        return new ComponentConfig[0];
    }

    
    public Object getComponent(final ComponentConfig componentConfig) {
        return null;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    
    @Override
    public MessageBus getMessageBus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dispose() {
    }

    
    @Override
    public <T> T[] getExtensions( final ExtensionPointName<T> extensionPointName) {
        throw new UnsupportedOperationException("getExtensions()");
    }

    public ComponentConfig getConfig(Class componentImplementation) {
        throw new UnsupportedOperationException("Method getConfig not implemented in " + getClass());
    }
}
