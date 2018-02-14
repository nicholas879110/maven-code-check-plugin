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

package com.gome.maven.openapi.projectRoots.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.projectRoots.*;
import com.gome.maven.openapi.projectRoots.ex.ProjectRoot;
import com.gome.maven.openapi.projectRoots.ex.ProjectRootContainer;
import com.gome.maven.openapi.roots.OrderRootType;
import com.gome.maven.openapi.roots.RootProvider;
import com.gome.maven.openapi.roots.impl.RootProviderBaseImpl;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.vfs.StandardFileSystems;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.containers.ContainerUtil;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

public class ProjectJdkImpl extends UserDataHolderBase implements JDOMExternalizable, Sdk, SdkModificator {
    private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.projectRoots.impl.ProjectJdkImpl");
    private final ProjectRootContainerImpl myRootContainer;
    private String myName;
    private String myVersionString;
    private boolean myVersionDefined = false;
    private String myHomePath = "";
    private final MyRootProvider myRootProvider = new MyRootProvider();
    private ProjectJdkImpl myOrigin = null;
    private SdkAdditionalData myAdditionalData = null;
    private SdkTypeId mySdkType;
     public static final String ELEMENT_NAME = "name";
     public static final String ATTRIBUTE_VALUE = "value";
     public static final String ELEMENT_TYPE = "type";
     public static final String ELEMENT_VERSION = "version";
     private static final String ELEMENT_ROOTS = "roots";
     private static final String ELEMENT_ROOT = "root";
     private static final String ELEMENT_PROPERTY = "property";
     private static final String VALUE_JDKHOME = "jdkHome";
     private static final String ATTRIBUTE_FILE = "file";
     public static final String ELEMENT_HOMEPATH = "homePath";
     private static final String ELEMENT_ADDITIONAL = "additional";

    public ProjectJdkImpl(String name, SdkTypeId sdkType) {
        mySdkType = sdkType;
        myRootContainer = new ProjectRootContainerImpl(true);
        myName = name;
        myRootContainer.addProjectRootContainerListener(myRootProvider);
    }

    public ProjectJdkImpl(String name, SdkTypeId sdkType, String homePath, String version) {
        this(name, sdkType);
        myHomePath = homePath;
        myVersionString = version;
    }

    @Override
    
    public SdkTypeId getSdkType() {
        if (mySdkType == null) {
            mySdkType = ProjectJdkTable.getInstance().getDefaultSdkType();
        }
        return mySdkType;
    }

    @Override
    
    public String getName() {
        return myName;
    }

    @Override
    public void setName( String name) {
        myName = name;
    }

    @Override
    public final void setVersionString( String versionString) {
        myVersionString = versionString == null || versionString.isEmpty() ? null : versionString;
        myVersionDefined = true;
    }

    @Override
    public String getVersionString() {
        if (myVersionString == null && !myVersionDefined) {
            String homePath = getHomePath();
            if (homePath != null && !homePath.isEmpty()) {
                setVersionString(getSdkType().getVersionString(this));
            }
        }
        return myVersionString;
    }

    public final void resetVersionString() {
        myVersionDefined = false;
        myVersionString = null;
    }

    @Override
    public String getHomePath() {
        return myHomePath;
    }

    @Override
    public VirtualFile getHomeDirectory() {
        if (myHomePath == null) {
            return null;
        }
        return StandardFileSystems.local().findFileByPath(myHomePath);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        myName = element.getChild(ELEMENT_NAME).getAttributeValue(ATTRIBUTE_VALUE);
        final Element typeChild = element.getChild(ELEMENT_TYPE);
        final String sdkTypeName = typeChild != null? typeChild.getAttributeValue(ATTRIBUTE_VALUE) : null;
        if (sdkTypeName != null) {
            mySdkType = ProjectJdkTable.getInstance().getSdkTypeByName(sdkTypeName);
        }
        final Element version = element.getChild(ELEMENT_VERSION);

        // set version if it was cached (defined)
        // otherwise it will be null && undefined
        if (version != null) {
            setVersionString(version.getAttributeValue(ATTRIBUTE_VALUE));
        }
        else {
            myVersionDefined = false;
        }

        if (element.getAttribute(ELEMENT_VERSION) == null || !"2".equals(element.getAttributeValue(ELEMENT_VERSION))) {
            myRootContainer.startChange();
            myRootContainer.readOldVersion(element.getChild(ELEMENT_ROOTS));
            final List children = element.getChild(ELEMENT_ROOTS).getChildren(ELEMENT_ROOT);
            for (final Object aChildren : children) {
                Element root = (Element)aChildren;
                for (final Object o : root.getChildren(ELEMENT_PROPERTY)) {
                    Element prop = (Element)o;
                    if (ELEMENT_TYPE.equals(prop.getAttributeValue(ELEMENT_NAME)) && VALUE_JDKHOME.equals(prop.getAttributeValue(ATTRIBUTE_VALUE))) {
                        myHomePath = VirtualFileManager.extractPath(root.getAttributeValue(ATTRIBUTE_FILE));
                    }
                }
            }
            myRootContainer.finishChange();
        }
        else {
            myHomePath = element.getChild(ELEMENT_HOMEPATH).getAttributeValue(ATTRIBUTE_VALUE);
            myRootContainer.readExternal(element.getChild(ELEMENT_ROOTS));
        }

        final Element additional = element.getChild(ELEMENT_ADDITIONAL);
        if (additional != null) {
            LOG.assertTrue(mySdkType != null);
            myAdditionalData = mySdkType.loadAdditionalData(this, additional);
        }
        else {
            myAdditionalData = null;
        }
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        element.setAttribute(ELEMENT_VERSION, "2");

        final Element name = new Element(ELEMENT_NAME);
        name.setAttribute(ATTRIBUTE_VALUE, myName);
        element.addContent(name);

        if (mySdkType != null) {
            final Element sdkType = new Element(ELEMENT_TYPE);
            sdkType.setAttribute(ATTRIBUTE_VALUE, mySdkType.getName());
            element.addContent(sdkType);
        }

        if (myVersionString != null) {
            final Element version = new Element(ELEMENT_VERSION);
            version.setAttribute(ATTRIBUTE_VALUE, myVersionString);
            element.addContent(version);
        }

        final Element home = new Element(ELEMENT_HOMEPATH);
        home.setAttribute(ATTRIBUTE_VALUE, myHomePath);
        element.addContent(home);

        Element roots = new Element(ELEMENT_ROOTS);
        myRootContainer.writeExternal(roots);
        element.addContent(roots);

        Element additional = new Element(ELEMENT_ADDITIONAL);
        if (myAdditionalData != null) {
            LOG.assertTrue(mySdkType != null);
            mySdkType.saveAdditionalData(myAdditionalData, additional);
        }
        element.addContent(additional);
    }

    @Override
    public void setHomePath(String path) {
        final boolean changes = myHomePath == null? path != null : !myHomePath.equals(path);
        myHomePath = path;
        if (changes) {
            myVersionString = null; // clear cached value if home path changed
            myVersionDefined = false;
        }
    }

    @Override
    
    public Object clone() {
        ProjectJdkImpl newJdk = new ProjectJdkImpl("", mySdkType);
        copyTo(newJdk);
        return newJdk;
    }

    @Override
    
    public RootProvider getRootProvider() {
        return myRootProvider;
    }

    public void copyTo(ProjectJdkImpl dest) {
        final String name = getName();
        dest.setName(name);
        dest.setHomePath(getHomePath());
        if (myVersionDefined) {
            dest.setVersionString(getVersionString());
        }
        else {
            dest.resetVersionString();
        }
        dest.setSdkAdditionalData(getSdkAdditionalData());
        dest.myRootContainer.startChange();
        dest.myRootContainer.removeAllRoots();
        for(OrderRootType rootType: OrderRootType.getAllTypes()) {
            copyRoots(myRootContainer, dest.myRootContainer, rootType);
        }
        dest.myRootContainer.finishChange();
    }

    private static void copyRoots(ProjectRootContainer srcContainer, ProjectRootContainer destContainer, OrderRootType type){
        final ProjectRoot[] newRoots = srcContainer.getRoots(type);
        for (ProjectRoot newRoot : newRoots) {
            destContainer.addRoot(newRoot, type);
        }
    }

    private class MyRootProvider extends RootProviderBaseImpl implements ProjectRootListener {
        @Override
        
        public String[] getUrls( OrderRootType rootType) {
            final ProjectRoot[] rootFiles = myRootContainer.getRoots(rootType);
            final ArrayList<String> result = new ArrayList<String>();
            for (ProjectRoot rootFile : rootFiles) {
                ContainerUtil.addAll(result, rootFile.getUrls());
            }
            return ArrayUtil.toStringArray(result);
        }

        @Override
        
        public VirtualFile[] getFiles( final OrderRootType rootType) {
            return myRootContainer.getRootFiles(rootType);
        }

        private final List<RootSetChangedListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

        @Override
        public void addRootSetChangedListener( RootSetChangedListener listener) {
            assert !myListeners.contains(listener);
            myListeners.add(listener);
            super.addRootSetChangedListener(listener);
        }

        @Override
        public void addRootSetChangedListener( final RootSetChangedListener listener,  Disposable parentDisposable) {
            super.addRootSetChangedListener(listener, parentDisposable);
            Disposer.register(parentDisposable, new Disposable() {
                @Override
                public void dispose() {
                    removeRootSetChangedListener(listener);
                }
            });
        }

        @Override
        public void removeRootSetChangedListener( RootSetChangedListener listener) {
            super.removeRootSetChangedListener(listener);
            myListeners.remove(listener);
        }

        @Override
        public void rootsChanged() {
            if (myListeners.isEmpty()) {
                return;
            }
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    fireRootSetChanged();
                }
            });
        }
    }

    // SdkModificator implementation
    @Override
    
    public SdkModificator getSdkModificator() {
        ProjectJdkImpl sdk = (ProjectJdkImpl)clone();
        sdk.myOrigin = this;
        sdk.myRootContainer.startChange();
        sdk.update();
        return sdk;
    }

    @Override
    public void commitChanges() {
        LOG.assertTrue(isWritable());
        myRootContainer.finishChange();
        copyTo(myOrigin);
        myOrigin = null;
    }

    @Override
    public SdkAdditionalData getSdkAdditionalData() {
        return myAdditionalData;
    }

    @Override
    public void setSdkAdditionalData(SdkAdditionalData data) {
        myAdditionalData = data;
    }

    @Override
    public VirtualFile[] getRoots(OrderRootType rootType) {
        final ProjectRoot[] roots = myRootContainer.getRoots(rootType); // use getRoots() cause the data is most up-to-date there
        final List<VirtualFile> files = new ArrayList<VirtualFile>(roots.length);
        for (ProjectRoot root : roots) {
            ContainerUtil.addAll(files, root.getVirtualFiles());
        }
        return VfsUtilCore.toVirtualFileArray(files);
    }

    @Override
    public void addRoot(VirtualFile root, OrderRootType rootType) {
        myRootContainer.addRoot(root, rootType);
    }

    @Override
    public void removeRoot(VirtualFile root, OrderRootType rootType) {
        myRootContainer.removeRoot(root, rootType);
    }

    @Override
    public void removeRoots(OrderRootType rootType) {
        myRootContainer.removeAllRoots(rootType);
    }

    @Override
    public void removeAllRoots() {
        myRootContainer.removeAllRoots();
    }

    @Override
    public boolean isWritable() {
        return myOrigin != null;
    }

    public void update() {
        myRootContainer.update();
    }

    @Override
    public String toString() {
        return getName() + ": "+ getVersionString() + " ("+getHomePath()+")";
    }
}
