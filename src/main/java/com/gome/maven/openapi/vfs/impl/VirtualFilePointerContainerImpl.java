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
package com.gome.maven.openapi.vfs.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.impl.ApplicationInfoImpl;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.TraceableDisposable;
import com.gome.maven.openapi.util.Trinity;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointer;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointerContainer;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointerListener;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointerManager;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.containers.ContainerUtil;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author dsl
 */
public class VirtualFilePointerContainerImpl extends TraceableDisposable implements VirtualFilePointerContainer, Disposable {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vfs.pointers.VirtualFilePointerContainer");
     private final List<VirtualFilePointer> myList = ContainerUtil.createLockFreeCopyOnWriteList();
     private final VirtualFilePointerManager myVirtualFilePointerManager;
     private final Disposable myParent;
    private final VirtualFilePointerListener myListener;
    private volatile Trinity<String[], VirtualFile[], VirtualFile[]> myCachedThings;
    private volatile long myTimeStampOfCachedThings = -1;
     public static final String URL_ATTR = "url";
    private boolean myDisposed;
    private static final boolean TRACE_CREATION = LOG.isDebugEnabled() || ApplicationManager.getApplication().isUnitTestMode();
    public VirtualFilePointerContainerImpl( VirtualFilePointerManager manager,
                                            Disposable parentDisposable,
                                            VirtualFilePointerListener listener) {
        //noinspection HardCodedStringLiteral
        super(TRACE_CREATION && !ApplicationInfoImpl.isInPerformanceTest()
                ? new Throwable("parent = '" + parentDisposable + "' (" + parentDisposable.getClass() + "); listener=" + listener)
                : null);
        myVirtualFilePointerManager = manager;
        myParent = parentDisposable;
        myListener = listener;
    }

    @Override
    public void readExternal( final Element rootChild,  final String childElements) throws InvalidDataException {
        final List urls = rootChild.getChildren(childElements);
        for (Object url : urls) {
            Element pathElement = (Element)url;
            final String urlAttribute = pathElement.getAttributeValue(URL_ATTR);
            if (urlAttribute == null) throw new InvalidDataException("path element without url");
            add(urlAttribute);
        }
    }

    @Override
    public void writeExternal( final Element element,  final String childElementName) {
        for (VirtualFilePointer pointer : myList) {
            String url = pointer.getUrl();
            final Element rootPathElement = new Element(childElementName);
            rootPathElement.setAttribute(URL_ATTR, url);
            element.addContent(rootPathElement);
        }
    }

    @Override
    public void moveUp( String url) {
        int index = indexOf(url);
        if (index <= 0) return;
        dropCaches();
        ContainerUtil.swapElements(myList, index - 1, index);
    }

    @Override
    public void moveDown( String url) {
        int index = indexOf(url);
        if (index < 0 || index + 1 >= myList.size()) return;
        dropCaches();
        ContainerUtil.swapElements(myList, index, index + 1);
    }

    private int indexOf( final String url) {
        for (int i = 0; i < myList.size(); i++) {
            final VirtualFilePointer pointer = myList.get(i);
            if (url.equals(pointer.getUrl())) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void killAll() {
        myList.clear();
    }

    @Override
    public void add( VirtualFile file) {
        assert !myDisposed;
        dropCaches();
        final VirtualFilePointer pointer = create(file);
        myList.add(pointer);
    }

    @Override
    public void add( String url) {
        assert !myDisposed;
        dropCaches();
        final VirtualFilePointer pointer = create(url);
        myList.add(pointer);
    }

    @Override
    public void remove( VirtualFilePointer pointer) {
        assert !myDisposed;
        dropCaches();
        final boolean result = myList.remove(pointer);
        LOG.assertTrue(result);
    }

    @Override
    
    public List<VirtualFilePointer> getList() {
        assert !myDisposed;
        return Collections.unmodifiableList(myList);
    }

    @Override
    public void addAll( VirtualFilePointerContainer that) {
        assert !myDisposed;
        dropCaches();

        List<VirtualFilePointer> thatList = ((VirtualFilePointerContainerImpl)that).myList;
        for (final VirtualFilePointer pointer : thatList) {
            myList.add(duplicate(pointer));
        }
    }

    private void dropCaches() {
        myTimeStampOfCachedThings = -1; // make it never equal to myVirtualFilePointerManager.getModificationCount()
        myCachedThings = EMPTY;
    }

    @Override
    
    public String[] getUrls() {
        return getOrCache().first;
    }

    
    private Trinity<String[], VirtualFile[], VirtualFile[]> getOrCache() {
        assert !myDisposed;
        long timeStamp = myTimeStampOfCachedThings;
        Trinity<String[], VirtualFile[], VirtualFile[]> cached = myCachedThings;
        return timeStamp == myVirtualFilePointerManager.getModificationCount() ? cached : cacheThings();
    }

    private static final Trinity<String[], VirtualFile[], VirtualFile[]> EMPTY =
            Trinity.create(ArrayUtil.EMPTY_STRING_ARRAY, VirtualFile.EMPTY_ARRAY, VirtualFile.EMPTY_ARRAY);

    
    private Trinity<String[], VirtualFile[], VirtualFile[]> cacheThings() {
        Trinity<String[], VirtualFile[], VirtualFile[]> result;
        if (myList.isEmpty()) {
            result = EMPTY;
        }
        else {
            List<VirtualFile> cachedFiles = new ArrayList<VirtualFile>(myList.size());
            List<String> cachedUrls = new ArrayList<String>(myList.size());
            List<VirtualFile> cachedDirectories = new ArrayList<VirtualFile>(myList.size() / 3);
            boolean allFilesAreDirs = true;
            for (VirtualFilePointer v : myList) {
                VirtualFile file = v.getFile();
                String url = v.getUrl();
                cachedUrls.add(url);
                if (file != null) {
                    cachedFiles.add(file);
                    if (file.isDirectory()) {
                        cachedDirectories.add(file);
                    }
                    else {
                        allFilesAreDirs = false;
                    }
                }
            }
            VirtualFile[] directories = VfsUtilCore.toVirtualFileArray(cachedDirectories);
            VirtualFile[] files = allFilesAreDirs ? directories : VfsUtilCore.toVirtualFileArray(cachedFiles);
            String[] urlsArray = ArrayUtil.toStringArray(cachedUrls);
            result = Trinity.create(urlsArray, files, directories);
        }
        myCachedThings = result;
        myTimeStampOfCachedThings = myVirtualFilePointerManager.getModificationCount();
        return result;
    }

    @Override
    
    public VirtualFile[] getFiles() {
        return getOrCache().second;
    }

    @Override
    
    public VirtualFile[] getDirectories() {
        return getOrCache().third;
    }

    @Override
    
    public VirtualFilePointer findByUrl( String url) {
        assert !myDisposed;
        for (VirtualFilePointer pointer : myList) {
            if (url.equals(pointer.getUrl())) return pointer;
        }
        return null;
    }

    @Override
    public void clear() {
        dropCaches();
        killAll();
    }

    @Override
    public int size() {
        return myList.size();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VirtualFilePointerContainerImpl)) return false;

        final VirtualFilePointerContainerImpl virtualFilePointerContainer = (VirtualFilePointerContainerImpl)o;

        return myList.equals(virtualFilePointerContainer.myList);
    }

    public int hashCode() {
        return myList.hashCode();
    }

    protected VirtualFilePointer create( VirtualFile file) {
        return myVirtualFilePointerManager.create(file, myParent, myListener);
    }

    protected VirtualFilePointer create( String url) {
        return myVirtualFilePointerManager.create(url, myParent, myListener);
    }

    protected VirtualFilePointer duplicate( VirtualFilePointer virtualFilePointer) {
        return myVirtualFilePointerManager.duplicate(virtualFilePointer, myParent, myListener);
    }

    
    
    @Override
    public String toString() {
        return "VFPContainer: " + myList/*+"; parent:"+myParent*/;
    }

    @Override
    
    public VirtualFilePointerContainer clone( Disposable parent) {
        return clone(parent, null);
    }

    @Override
    
    public VirtualFilePointerContainer clone( Disposable parent,  VirtualFilePointerListener listener) {
        assert !myDisposed;
        VirtualFilePointerContainer clone = myVirtualFilePointerManager.createContainer(parent, listener);
        for (VirtualFilePointer pointer : myList) {
            clone.add(pointer.getUrl());
        }
        return clone;
    }

    @Override
    public void dispose() {
        assert !myDisposed;
        myDisposed = true;
        kill(null);
    }
}
