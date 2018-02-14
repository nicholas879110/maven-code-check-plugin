/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.gome.maven.openapi.roots.impl;

import com.gome.maven.openapi.projectRoots.ProjectJdkTable;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.roots.ModuleJdkOrderEntry;
import com.gome.maven.openapi.roots.OrderEntry;
import com.gome.maven.openapi.roots.RootPolicy;
import com.gome.maven.openapi.roots.RootProvider;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointerManager;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer;

/**
 * @author dsl
 */
public class ModuleJdkOrderEntryImpl extends LibraryOrderEntryBaseImpl implements WritableOrderEntry,
        ClonableOrderEntry,
        ModuleJdkOrderEntry,
        ProjectJdkTable.Listener {
     public static final String ENTRY_TYPE = JpsModuleRootModelSerializer.JDK_TYPE;
     public static final String JDK_NAME_ATTR = JpsModuleRootModelSerializer.JDK_NAME_ATTRIBUTE;
     public static final String JDK_TYPE_ATTR = JpsModuleRootModelSerializer.JDK_TYPE_ATTRIBUTE;

     private Sdk myJdk;
    private String myJdkName;
    private String myJdkType;

    ModuleJdkOrderEntryImpl( Sdk projectJdk,  RootModelImpl rootModel,  ProjectRootManagerImpl projectRootManager) {
        super(rootModel, projectRootManager);
        init(projectJdk, null, null);
    }

    ModuleJdkOrderEntryImpl( Element element,  RootModelImpl rootModel,  ProjectRootManagerImpl projectRootManager) throws InvalidDataException {
        super(rootModel, projectRootManager);
        if (!element.getName().equals(OrderEntryFactory.ORDER_ENTRY_ELEMENT_NAME)) {
            throw new InvalidDataException();
        }
        final Attribute jdkNameAttribute = element.getAttribute(JDK_NAME_ATTR);
        if (jdkNameAttribute == null) {
            throw new InvalidDataException();
        }

        final String jdkName = jdkNameAttribute.getValue();
        final String jdkType = element.getAttributeValue(JDK_TYPE_ATTR);
        final Sdk jdkByName = findJdk(jdkName, jdkType);
        if (jdkByName == null) {
            init(null, jdkName, jdkType);
        }
        else {
            init(jdkByName, null, null);
        }
    }

    
    private static Sdk findJdk(final String sdkName, final String sdkType) {
        for (SdkFinder sdkFinder : SdkFinder.EP_NAME.getExtensions()) {
            final Sdk sdk = sdkFinder.findSdk(sdkName, sdkType);
            if (sdk != null) {
                return sdk;
            }
        }
        final ProjectJdkTable projectJdkTable = ProjectJdkTable.getInstance();
        return projectJdkTable.findJdk(sdkName, sdkType);
    }


    private ModuleJdkOrderEntryImpl( ModuleJdkOrderEntryImpl that,  RootModelImpl rootModel,  ProjectRootManagerImpl projectRootManager) {
        super(rootModel, projectRootManager);
        init(that.myJdk, that.getJdkName(), that.getJdkType());
    }

    public ModuleJdkOrderEntryImpl(final String jdkName,
                                   final String jdkType,
                                    final RootModelImpl rootModel,
                                    final ProjectRootManagerImpl projectRootManager) {
        super(rootModel, projectRootManager);
        init(null, jdkName, jdkType);
    }

    private void init(final Sdk jdk, final String jdkName, final String jdkType) {
        myJdk = jdk;
        setJdkName(jdkName);
        setJdkType(jdkType);
        addListener();
        init();
    }

    private String getJdkType() {
        if (myJdk != null){
            return myJdk.getSdkType().getName();
        }
        return myJdkType;
    }

    private void addListener() {
        myProjectRootManagerImpl.addJdkTableListener(this);
    }

    @Override
    protected RootProvider getRootProvider() {
        return myJdk == null ? null : myJdk.getRootProvider();
    }

    @Override
    
    public Sdk getJdk() {
        return getRootModel().getConfigurationAccessor().getSdk(myJdk, myJdkName);
    }

    @Override
    
    public String getJdkName() {
        if (myJdkName != null) return myJdkName;
        Sdk jdk = getJdk();
        if (jdk != null) {
            return jdk.getName();
        }
        return null;
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }


    @Override
    
    public String getPresentableName() {
        return "< " + (myJdk == null ? getJdkName() : myJdk.getName())+ " >";
    }

    @Override
    public boolean isValid() {
        return !isDisposed() && getJdk() != null;
    }

    @Override
    public <R> R accept( RootPolicy<R> policy, R initialValue) {
        return policy.visitModuleJdkOrderEntry(this, initialValue);
    }

    @Override
    public void jdkAdded( Sdk jdk) {
        if (myJdk == null && getJdkName().equals(jdk.getName())) {
            myJdk = jdk;
            setJdkName(null);
            setJdkType(null);
            updateFromRootProviderAndSubscribe();
        }
    }

    @Override
    public void jdkNameChanged( Sdk jdk, String previousName) {
        if (myJdk == null && getJdkName().equals(jdk.getName())) {
            myJdk = jdk;
            setJdkName(null);
            setJdkType(null);
            updateFromRootProviderAndSubscribe();
        }
    }

    @Override
    public void jdkRemoved(Sdk jdk) {
        if (jdk == myJdk) {
            setJdkName(myJdk.getName());
            setJdkType(myJdk.getSdkType().getName());
            myJdk = null;
            updateFromRootProviderAndSubscribe();
        }
    }

    @Override
    public void writeExternal( Element rootElement) throws WriteExternalException {
        final Element element = OrderEntryFactory.createOrderEntryElement(ENTRY_TYPE);
        final String jdkName = getJdkName();
        if (jdkName != null) {
            element.setAttribute(JDK_NAME_ATTR, jdkName);
        }
        final String jdkType = getJdkType();
        if (jdkType != null) {
            element.setAttribute(JDK_TYPE_ATTR, jdkType);
        }
        rootElement.addContent(element);
    }

    @Override
    
    public OrderEntry cloneEntry( RootModelImpl rootModel,
                                 ProjectRootManagerImpl projectRootManager,
                                 VirtualFilePointerManager filePointerManager) {
        return new ModuleJdkOrderEntryImpl(this, rootModel, ProjectRootManagerImpl.getInstanceImpl(getRootModel().getModule().getProject()));
    }

    @Override
    public void dispose() {
        super.dispose();
        myProjectRootManagerImpl.removeJdkTableListener(this);
    }

    private void setJdkName(String jdkName) {
        myJdkName = jdkName;
    }

    private void setJdkType(String jdkType) {
        myJdkType = jdkType;
    }
}
