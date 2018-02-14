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
package com.gome.maven.openapi.components.impl;

import com.gome.maven.application.options.PathMacrosCollector;
import com.gome.maven.application.options.PathMacrosImpl;
import com.gome.maven.application.options.ReplacePathToMacroMap;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.PathMacros;
import com.gome.maven.openapi.components.CompositePathMacroFilter;
import com.gome.maven.openapi.components.ExpandMacroToPathMap;
import com.gome.maven.openapi.components.PathMacroManager;
import com.gome.maven.openapi.components.TrackingPathMacroSubstitutor;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.StandardFileSystems;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.VirtualFileSystem;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.MultiMap;
import com.gome.maven.util.containers.SmartHashSet;
import gnu.trove.THashSet;
import org.jdom.Element;
import org.jetbrains.jps.model.serialization.PathMacroUtil;


import java.util.*;

public class BasePathMacroManager extends PathMacroManager {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.components.impl.BasePathMacroManager");
    private static final CompositePathMacroFilter FILTER = new CompositePathMacroFilter(Extensions.getExtensions(PathMacrosCollector.MACRO_FILTER_EXTENSION_POINT_NAME));

    private PathMacrosImpl myPathMacros;

    public BasePathMacroManager( PathMacros pathMacros) {
        myPathMacros = (PathMacrosImpl)pathMacros;
    }

    protected static void addFileHierarchyReplacements(ExpandMacroToPathMap result, String macroName,  String path) {
        if (path != null) {
            addFileHierarchyReplacements(result, getLocalFileSystem().findFileByPath(path), '$' + macroName + '$');
        }
    }

    private static void addFileHierarchyReplacements(ExpandMacroToPathMap result,  VirtualFile f, String macro) {
        if (f == null) {
            return;
        }

        addFileHierarchyReplacements(result, f.getParent(), macro + "/..");
        result.put(macro, StringUtil.trimEnd(f.getPath(), "/"));
    }

    protected static void addFileHierarchyReplacements(ReplacePathToMacroMap result, String macroName,  String path,  String stopAt) {
        if (path == null) {
            return;
        }

        String macro = '$' + macroName + '$';
        path = StringUtil.trimEnd(FileUtil.toSystemIndependentName(path), "/");
        boolean overwrite = true;
        while (StringUtil.isNotEmpty(path) && path.contains("/")) {
            result.addReplacement(path, macro, overwrite);

            if (path.equals(stopAt)) {
                break;
            }

            macro += "/..";
            overwrite = false;
            path = StringUtil.getPackageName(path, '/');
        }
    }

    private static VirtualFileSystem getLocalFileSystem() {
        // Use VFM directly because of mocks in tests.
        return VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL);
    }

    public ExpandMacroToPathMap getExpandMacroMap() {
        ExpandMacroToPathMap result = new ExpandMacroToPathMap();
        for (Map.Entry<String, String> entry : PathMacroUtil.getGlobalSystemMacros().entrySet()) {
            result.addMacroExpand(entry.getKey(), entry.getValue());
        }
        getPathMacros().addMacroExpands(result);
        return result;
    }

    protected ReplacePathToMacroMap getReplacePathMap() {
        ReplacePathToMacroMap result = new ReplacePathToMacroMap();
        for (Map.Entry<String, String> entry : PathMacroUtil.getGlobalSystemMacros().entrySet()) {
            result.addMacroReplacement(entry.getValue(), entry.getKey());
        }
        getPathMacros().addMacroReplacements(result);
        return result;
    }

    @Override
    public TrackingPathMacroSubstitutor createTrackingSubstitutor() {
        return new MyTrackingPathMacroSubstitutor();
    }

    @Override
    public String expandPath(final String path) {
        return getExpandMacroMap().substitute(path, SystemInfo.isFileSystemCaseSensitive);
    }

    @Override
    public String collapsePath( String path) {
        return getReplacePathMap().substitute(path, SystemInfo.isFileSystemCaseSensitive);
    }

    @Override
    public void collapsePathsRecursively( final Element element) {
        getReplacePathMap().substitute(element, SystemInfo.isFileSystemCaseSensitive, true);
    }

    @Override
    public String collapsePathsRecursively( final String text) {
        return getReplacePathMap().substituteRecursively(text, SystemInfo.isFileSystemCaseSensitive);
    }

    @Override
    public void expandPaths( final Element element) {
        getExpandMacroMap().substitute(element, SystemInfo.isFileSystemCaseSensitive);
    }

    @Override
    public void collapsePaths( final Element element) {
        getReplacePathMap().substitute(element, SystemInfo.isFileSystemCaseSensitive);
    }

    public PathMacrosImpl getPathMacros() {
        if (myPathMacros == null) {
            myPathMacros = PathMacrosImpl.getInstanceEx();
        }
        return myPathMacros;
    }

    private class MyTrackingPathMacroSubstitutor implements TrackingPathMacroSubstitutor {
        private final String myLock = new String("MyTrackingPathMacroSubstitutor.lock");

        private final MultiMap<String, String> myMacroToComponentNames = MultiMap.createSet();
        private final MultiMap<String, String> myComponentNameToMacros = MultiMap.createSet();

        public MyTrackingPathMacroSubstitutor() {
        }

        @Override
        public void reset() {
            synchronized (myLock) {
                myMacroToComponentNames.clear();
                myComponentNameToMacros.clear();
            }
        }

        @Override
        public String expandPath(final String path) {
            return getExpandMacroMap().substitute(path, SystemInfo.isFileSystemCaseSensitive);
        }

        @Override
        public String collapsePath( String path) {
            return getReplacePathMap().substitute(path, SystemInfo.isFileSystemCaseSensitive);
        }

        @Override
        public void expandPaths( final Element element) {
            getExpandMacroMap().substitute(element, SystemInfo.isFileSystemCaseSensitive);
        }

        @Override
        public void collapsePaths( final Element element) {
            getReplacePathMap().substitute(element, SystemInfo.isFileSystemCaseSensitive, false, FILTER);
        }

        public int hashCode() {
            return getExpandMacroMap().hashCode();
        }

        @Override
        public void invalidateUnknownMacros( Set<String> macros) {
            synchronized (myLock) {
                for (String macro : macros) {
                    Collection<String> componentNames = myMacroToComponentNames.remove(macro);
                    if (!ContainerUtil.isEmpty(componentNames)) {
                        for (String component : componentNames) {
                            myComponentNameToMacros.remove(component);
                        }
                    }
                }
            }
        }

        
        @Override
        public Collection<String> getComponents( Collection<String> macros) {
            synchronized (myLock) {
                Set<String> result = new SmartHashSet<String>();
                for (String macro : macros) {
                    result.addAll(myMacroToComponentNames.get(macro));
                }
                return result;
            }
        }

        
        @Override
        public Collection<String> getUnknownMacros( String componentName) {
            synchronized (myLock) {
                Collection<String> list = componentName == null ? myMacroToComponentNames.keySet() : myComponentNameToMacros.get(componentName);
                return ContainerUtil.isEmpty(list) ? Collections.<String>emptyList() : new THashSet<String>(list);
            }
        }

        @Override
        public void addUnknownMacros( String componentName,  Collection<String> unknownMacros) {
            if (unknownMacros.isEmpty()) {
                return;
            }

            LOG.debug("Registering unknown macros " + new ArrayList<String>(unknownMacros) + " in component " + componentName);

            synchronized (myLock) {
                for (String unknownMacro : unknownMacros) {
                    myMacroToComponentNames.putValue(unknownMacro, componentName);
                }

                myComponentNameToMacros.putValues(componentName, unknownMacros);
            }
        }
    }

    protected static boolean pathsEqual( String path1,  String path2) {
        return path1 != null && path2 != null &&
                FileUtil.pathsEqual(FileUtil.toSystemIndependentName(path1), FileUtil.toSystemIndependentName(path2));
    }
}
