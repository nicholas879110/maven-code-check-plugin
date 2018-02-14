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

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.roots.ContentEntry;
import com.gome.maven.openapi.roots.ContentFolder;
import com.gome.maven.openapi.roots.ExcludeFolder;
import com.gome.maven.openapi.roots.SourceFolder;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointer;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointerManager;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer;

import java.util.*;

/**
 *  @author dsl
 */
public class ContentEntryImpl extends RootModelComponentBase implements ContentEntry, ClonableContentEntry, Comparable<ContentEntryImpl> {
    private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.roots.impl.SimpleContentEntryImpl");
     private final VirtualFilePointer myRoot;
     public static final String ELEMENT_NAME = JpsModuleRootModelSerializer.CONTENT_TAG;
    private final Set<SourceFolder> mySourceFolders = new LinkedHashSet<SourceFolder>();
    private final Set<ExcludeFolder> myExcludeFolders = new TreeSet<ExcludeFolder>(ContentFolderComparator.INSTANCE);
     public static final String URL_ATTRIBUTE = JpsModuleRootModelSerializer.URL_ATTRIBUTE;

    ContentEntryImpl( VirtualFile file,  RootModelImpl m) {
        this(file.getUrl(), m);
    }

    ContentEntryImpl( String url,  RootModelImpl m) {
        super(m);
        myRoot = VirtualFilePointerManager.getInstance().create(url, this, null);
    }

    ContentEntryImpl( Element e,  RootModelImpl m) throws InvalidDataException {
        this(getUrlFrom(e), m);
        initSourceFolders(e);
        initExcludeFolders(e);
    }

    private static String getUrlFrom( Element e) throws InvalidDataException {
        LOG.assertTrue(ELEMENT_NAME.equals(e.getName()));

        String url = e.getAttributeValue(URL_ATTRIBUTE);
        if (url == null) throw new InvalidDataException();
        return url;
    }

    private void initSourceFolders( Element e) throws InvalidDataException {
        for (Object child : e.getChildren(SourceFolderImpl.ELEMENT_NAME)) {
            addSourceFolder(new SourceFolderImpl((Element)child, this));
        }
    }

    private void initExcludeFolders( Element e) throws InvalidDataException {
        for (Object child : e.getChildren(ExcludeFolderImpl.ELEMENT_NAME)) {
            ExcludeFolderImpl excludeFolder = new ExcludeFolderImpl((Element)child, this);
            addExcludeFolder(excludeFolder);
        }
    }

    @Override
    public VirtualFile getFile() {
        //assert !isDisposed();
        return myRoot.getFile();
    }

    @Override
    
    public String getUrl() {
        return myRoot.getUrl();
    }

    
    @Override
    public SourceFolder[] getSourceFolders() {
        return mySourceFolders.toArray(new SourceFolder[mySourceFolders.size()]);
    }

    
    @Override
    public List<SourceFolder> getSourceFolders( JpsModuleSourceRootType<?> rootType) {
        return getSourceFolders(Collections.singleton(rootType));
    }

    
    @Override
    public List<SourceFolder> getSourceFolders( Set<? extends JpsModuleSourceRootType<?>> rootTypes) {
        SmartList<SourceFolder> folders = new SmartList<SourceFolder>();
        for (SourceFolder folder : mySourceFolders) {
            if (rootTypes.contains(folder.getRootType())) {
                folders.add(folder);
            }
        }
        return folders;
    }

    @Override
    
    public VirtualFile[] getSourceFolderFiles() {
        assert !isDisposed();
        final SourceFolder[] sourceFolders = getSourceFolders();
        ArrayList<VirtualFile> result = new ArrayList<VirtualFile>(sourceFolders.length);
        for (SourceFolder sourceFolder : sourceFolders) {
            final VirtualFile file = sourceFolder.getFile();
            if (file != null) {
                result.add(file);
            }
        }
        return VfsUtilCore.toVirtualFileArray(result);
    }

    
    @Override
    public ExcludeFolder[] getExcludeFolders() {
        //assert !isDisposed();
        return myExcludeFolders.toArray(new ExcludeFolder[myExcludeFolders.size()]);
    }

    
    @Override
    public List<String> getExcludeFolderUrls() {
        List<String> excluded = new ArrayList<String>();
        for (ExcludeFolder folder : myExcludeFolders) {
            excluded.add(folder.getUrl());
        }
        for (DirectoryIndexExcludePolicy excludePolicy : Extensions.getExtensions(DirectoryIndexExcludePolicy.EP_NAME, getRootModel().getProject())) {
            for (VirtualFilePointer pointer : excludePolicy.getExcludeRootsForModule(getRootModel())) {
                excluded.add(pointer.getUrl());
            }
        }
        return excluded;
    }

    @Override
    
    public VirtualFile[] getExcludeFolderFiles() {
        assert !isDisposed();
        ArrayList<VirtualFile> result = new ArrayList<VirtualFile>();
        for (ExcludeFolder excludeFolder : getExcludeFolders()) {
            ContainerUtil.addIfNotNull(result, excludeFolder.getFile());
        }
        for (DirectoryIndexExcludePolicy excludePolicy : Extensions.getExtensions(DirectoryIndexExcludePolicy.EP_NAME, getRootModel().getProject())) {
            for (VirtualFilePointer pointer : excludePolicy.getExcludeRootsForModule(getRootModel())) {
                ContainerUtil.addIfNotNull(result, pointer.getFile());
            }
        }
        return VfsUtilCore.toVirtualFileArray(result);
    }

    
    @Override
    public SourceFolder addSourceFolder( VirtualFile file, boolean isTestSource) {
        return addSourceFolder(file, isTestSource, SourceFolderImpl.DEFAULT_PACKAGE_PREFIX);
    }

    
    @Override
    public SourceFolder addSourceFolder( VirtualFile file, boolean isTestSource,  String packagePrefix) {
        JavaSourceRootType type = isTestSource ? JavaSourceRootType.TEST_SOURCE : JavaSourceRootType.SOURCE;
        return addSourceFolder(file, type);
    }

    @Override
    
    public <P extends JpsElement> SourceFolder addSourceFolder( VirtualFile file,  JpsModuleSourceRootType<P> type,
                                                                P properties) {
        assertCanAddFolder(file);
        return addSourceFolder(new SourceFolderImpl(file, JpsElementFactory.getInstance().createModuleSourceRoot(file.getUrl(), type, properties), this));
    }

    
    @Override
    public <P extends JpsElement> SourceFolder addSourceFolder( VirtualFile file,  JpsModuleSourceRootType<P> type) {
        return addSourceFolder(file, type, type.createDefaultProperties());
    }

    
    @Override
    public SourceFolder addSourceFolder( String url, boolean isTestSource) {
        return addSourceFolder(url, isTestSource ? JavaSourceRootType.TEST_SOURCE : JavaSourceRootType.SOURCE);
    }

    
    @Override
    public <P extends JpsElement> SourceFolder addSourceFolder( String url,  JpsModuleSourceRootType<P> type) {
        return addSourceFolder(url, type, type.createDefaultProperties());
    }

    
    @Override
    public <P extends JpsElement> SourceFolder addSourceFolder( String url,
                                                                JpsModuleSourceRootType<P> type,
                                                                P properties) {
        assertFolderUnderMe(url);
        JpsModuleSourceRoot sourceRoot = JpsElementFactory.getInstance().createModuleSourceRoot(url, type, properties);
        return addSourceFolder(new SourceFolderImpl(sourceRoot, this));
    }

    
    private SourceFolder addSourceFolder( SourceFolderImpl f) {
        mySourceFolders.add(f);
        Disposer.register(this, f); //rewire source folder dispose parent from rootmodel to this content root
        return f;
    }

    @Override
    public void removeSourceFolder( SourceFolder sourceFolder) {
        assert !isDisposed();
        assertCanRemoveFrom(sourceFolder, mySourceFolders);
        doRemove(sourceFolder);
    }

    private void doRemove(SourceFolder sourceFolder) {
        mySourceFolders.remove(sourceFolder);
        Disposer.dispose((Disposable)sourceFolder);
    }

    @Override
    public void clearSourceFolders() {
        assert !isDisposed();
        getRootModel().assertWritable();
        for (SourceFolder folder : mySourceFolders) {
            Disposer.dispose((Disposable)folder);
        }
        mySourceFolders.clear();
    }

    @Override
    public ExcludeFolder addExcludeFolder( VirtualFile file) {
        assert !isDisposed();
        assertCanAddFolder(file);
        return addExcludeFolder(new ExcludeFolderImpl(file, this));
    }

    @Override
    public ExcludeFolder addExcludeFolder( String url) {
        assert !isDisposed();
        assertCanAddFolder(url);
        return addExcludeFolder(new ExcludeFolderImpl(url, this));
    }

    private void assertCanAddFolder( VirtualFile file) {
        assertCanAddFolder(file.getUrl());
    }

    private void assertCanAddFolder( String url) {
        getRootModel().assertWritable();
        assertFolderUnderMe(url);
    }

    @Override
    public void removeExcludeFolder( ExcludeFolder excludeFolder) {
        assert !isDisposed();
        assertCanRemoveFrom(excludeFolder, myExcludeFolders);
        myExcludeFolders.remove(excludeFolder);
        Disposer.dispose((Disposable)excludeFolder);
    }

    @Override
    public boolean removeExcludeFolder( String url) {
        for (ExcludeFolder folder : myExcludeFolders) {
            if (folder.getUrl().equals(url)) {
                myExcludeFolders.remove(folder);
                Disposer.dispose((Disposable)folder);
                return true;
            }
        }
        return false;
    }

    @Override
    public void clearExcludeFolders() {
        assert !isDisposed();
        getRootModel().assertWritable();
        for (ExcludeFolder excludeFolder : myExcludeFolders) {
            Disposer.dispose((Disposable)excludeFolder);
        }
        myExcludeFolders.clear();
    }

    private ExcludeFolder addExcludeFolder(ExcludeFolder f) {
        Disposer.register(this, (Disposable)f);
        myExcludeFolders.add(f);
        return f;
    }

    private <T extends ContentFolder> void assertCanRemoveFrom(T f,  Set<T> ff) {
        getRootModel().assertWritable();
        LOG.assertTrue(ff.contains(f));
    }

    private void assertFolderUnderMe( String url) {
        final String path = VfsUtilCore.urlToPath(url);
        final String rootPath = VfsUtilCore.urlToPath(getUrl());
        if (!FileUtil.isAncestor(rootPath, path, false)) {
            LOG.error("The file '" + path + "' is not under content entry root '" + rootPath + "'");
        }
    }

    @Override
    public boolean isSynthetic() {
        return false;
    }

    @Override
    
    public ContentEntry cloneEntry( RootModelImpl rootModel) {
        assert !isDisposed();
        ContentEntryImpl cloned = new ContentEntryImpl(myRoot.getUrl(), rootModel);
        for (final SourceFolder sourceFolder : mySourceFolders) {
            if (sourceFolder instanceof ClonableContentFolder) {
                ContentFolder folder = ((ClonableContentFolder)sourceFolder).cloneFolder(cloned);
                cloned.addSourceFolder((SourceFolderImpl)folder);
            }
        }

        for (final ExcludeFolder excludeFolder : myExcludeFolders) {
            if (excludeFolder instanceof ClonableContentFolder) {
                ContentFolder folder = ((ClonableContentFolder)excludeFolder).cloneFolder(cloned);
                cloned.addExcludeFolder((ExcludeFolder)folder);
            }
        }

        return cloned;
    }

    public void writeExternal( Element element) throws WriteExternalException {
        assert !isDisposed();
        LOG.assertTrue(ELEMENT_NAME.equals(element.getName()));
        element.setAttribute(URL_ATTRIBUTE, myRoot.getUrl());
        for (final SourceFolder sourceFolder : mySourceFolders) {
            if (sourceFolder instanceof SourceFolderImpl) {
                JpsModuleRootModelSerializer.saveSourceRoot(element, sourceFolder.getUrl(), sourceFolder.getJpsElement().asTyped());
            }
        }

        for (final ExcludeFolder excludeFolder : myExcludeFolders) {
            if (excludeFolder instanceof ExcludeFolderImpl) {
                final Element subElement = new Element(ExcludeFolderImpl.ELEMENT_NAME);
                ((ExcludeFolderImpl)excludeFolder).writeExternal(subElement);
                element.addContent(subElement);
            }
        }
    }

    private static final class ContentFolderComparator implements Comparator<ContentFolder> {
        public static final ContentFolderComparator INSTANCE = new ContentFolderComparator();

        @Override
        public int compare( ContentFolder o1,  ContentFolder o2) {
            return o1.getUrl().compareTo(o2.getUrl());
        }
    }

    @Override
    public int compareTo( ContentEntryImpl other) {
        int i = getUrl().compareTo(other.getUrl());
        if (i != 0) return i;
        i = ArrayUtil.lexicographicCompare(getSourceFolders(), other.getSourceFolders());
        if (i != 0) return i;
        return ArrayUtil.lexicographicCompare(getExcludeFolders(), other.getExcludeFolders());
    }
}
