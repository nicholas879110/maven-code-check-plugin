/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.lang;

import com.gome.maven.injected.editor.VirtualFileWindow;
import com.gome.maven.openapi.components.PersistentStateComponent;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.roots.impl.FilePropertyPusher;
import com.gome.maven.openapi.roots.impl.PushedFilePropertiesUpdater;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import org.jdom.Element;

import java.util.*;

/**
 * @author gregsh
 */
public abstract class PerFileMappingsBase<T> implements PersistentStateComponent<Element>, PerFileMappings<T> {
    private final Map<VirtualFile, T> myMappings = ContainerUtil.newHashMap();

    
    protected FilePropertyPusher<T> getFilePropertyPusher() {
        return null;
    }

    
    protected Project getProject() { return null; }

    
    @Override
    public Map<VirtualFile, T> getMappings() {
        synchronized (myMappings) {
            cleanup();
            return Collections.unmodifiableMap(myMappings);
        }
    }

    private void cleanup() {
        for (final VirtualFile file : new ArrayList<VirtualFile>(myMappings.keySet())) {
            if (file != null //PROJECT, top-level
                    && !file.isValid()) {
                myMappings.remove(file);
            }
        }
    }

    @Override
    
    public T getMapping( VirtualFile file) {
        FilePropertyPusher<T> pusher = getFilePropertyPusher();
        T t = getMappingInner(file, myMappings, pusher == null? null : pusher.getFileDataKey());
        return t == null? getDefaultMapping(file) : t;
    }

    
    protected static <T> T getMappingInner( VirtualFile file,  Map<VirtualFile, T> mappings,  Key<T> pusherKey) {
        if (file instanceof VirtualFileWindow) {
            final VirtualFileWindow window = (VirtualFileWindow)file;
            file = window.getDelegate();
        }
        VirtualFile originalFile = file instanceof LightVirtualFile ? ((LightVirtualFile)file).getOriginalFile() : null;
        if (Comparing.equal(originalFile, file)) originalFile = null;

        if (file != null) {
            final T pushedValue = pusherKey == null? null : file.getUserData(pusherKey);
            if (pushedValue != null) return pushedValue;
        }
        if (originalFile != null) {
            final T pushedValue = pusherKey == null? null : originalFile.getUserData(pusherKey);
            if (pushedValue != null) return pushedValue;
        }
        if (mappings == null) return null;
        synchronized (mappings) {
            for (VirtualFile cur = file; ; cur = cur.getParent()) {
                T t = mappings.get(cur);
                if (t != null) return t;
                if (originalFile != null) {
                    t = mappings.get(originalFile);
                    if (t != null) return t;
                    originalFile = originalFile.getParent();
                }
                if (cur == null) break;
            }
        }
        return null;
    }

    @Override
    public T chosenToStored(VirtualFile file, T value) {
        return value;
    }

    @Override
    public boolean isSelectable(T value) {
        return true;
    }

    @Override
    
    public T getDefaultMapping( VirtualFile file) {
        return null;
    }

    
    public T getImmediateMapping( VirtualFile file) {
        synchronized (myMappings) {
            return myMappings.get(file);
        }
    }

    @Override
    public void setMappings( final Map<VirtualFile, T> mappings) {
        Collection<VirtualFile> oldFiles;
        synchronized (myMappings) {
            oldFiles = ContainerUtil.newArrayList(myMappings.keySet());
            myMappings.clear();
            myMappings.putAll(mappings);
            cleanup();
        }
        Project project = getProject();
        handleMappingChange(mappings.keySet(), oldFiles, project != null && !project.isDefault());
    }

    public void setMapping( final VirtualFile file,  T dialect) {
        synchronized (myMappings) {
            if (dialect == null) {
                myMappings.remove(file);
            }
            else {
                myMappings.put(file, dialect);
            }
        }
        List<VirtualFile> files = ContainerUtil.createMaybeSingletonList(file);
        handleMappingChange(files, files, false);
    }

    private void handleMappingChange(Collection<VirtualFile> files, Collection<VirtualFile> oldFiles, boolean includeOpenFiles) {
        Project project = getProject();
        FilePropertyPusher<T> pusher = getFilePropertyPusher();
        if (project != null && pusher != null) {
            for (VirtualFile oldFile : oldFiles) {
                if (oldFile == null) continue; // project
                oldFile.putUserData(pusher.getFileDataKey(), null);
            }
            PushedFilePropertiesUpdater updater = PushedFilePropertiesUpdater.getInstance(project);
            updater.pushAll(pusher);
        }
        if (shouldReparseFiles()) {
            Project[] projects = project == null ? ProjectManager.getInstance().getOpenProjects() : new Project[] { project };
            for (Project p : projects) {
                PsiDocumentManager.getInstance(p).reparseFiles(files, includeOpenFiles);
            }
        }
    }

    @Override
    public Collection<T> getAvailableValues(VirtualFile file) {
        return getAvailableValues();
    }

    protected abstract List<T> getAvailableValues();

    
    protected abstract String serialize(T t);

    @Override
    public Element getState() {
        synchronized (myMappings) {
            cleanup();
            final Element element = new Element("x");
            final List<VirtualFile> files = new ArrayList<VirtualFile>(myMappings.keySet());
            Collections.sort(files, new Comparator<VirtualFile>() {
                @Override
                public int compare(final VirtualFile o1, final VirtualFile o2) {
                    if (o1 == null || o2 == null) return o1 == null ? o2 == null ? 0 : 1 : -1;
                    return o1.getPath().compareTo(o2.getPath());
                }
            });
            for (VirtualFile file : files) {
                final T dialect = myMappings.get(file);
                String value = serialize(dialect);
                if (value != null) {
                    final Element child = new Element("file");
                    element.addContent(child);
                    child.setAttribute("url", file == null ? "PROJECT" : file.getUrl());
                    child.setAttribute(getValueAttribute(), value);
                }
            }
            return element;
        }
    }

    
    protected T handleUnknownMapping(VirtualFile file, String value) {
        return null;
    }

    
    protected String getValueAttribute() {
        return "value";
    }

    @Override
    public void loadState(final Element state) {
        synchronized (myMappings) {
            final THashMap<String, T> dialectMap = new THashMap<String, T>();
            for (T dialect : getAvailableValues()) {
                String key = serialize(dialect);
                if (key != null) {
                    dialectMap.put(key, dialect);
                }
            }
            myMappings.clear();
            final List<Element> files = state.getChildren("file");
            for (Element fileElement : files) {
                final String url = fileElement.getAttributeValue("url");
                final String dialectID = fileElement.getAttributeValue(getValueAttribute());
                final VirtualFile file = url.equals("PROJECT") ? null : VirtualFileManager.getInstance().findFileByUrl(url);
                T dialect = dialectMap.get(dialectID);
                if (dialect == null) {
                    dialect = handleUnknownMapping(file, dialectID);
                    if (dialect == null) continue;
                }
                if (file != null || url.equals("PROJECT")) {
                    myMappings.put(file, dialect);
                }
            }
        }
    }

    
    public void cleanupForNextTest() {
        synchronized (myMappings) {
            myMappings.clear();
        }
    }

    protected boolean shouldReparseFiles() {
        return true;
    }

    public boolean hasMappings() {
        synchronized (myMappings) {
            return !myMappings.isEmpty();
        }
    }
}
