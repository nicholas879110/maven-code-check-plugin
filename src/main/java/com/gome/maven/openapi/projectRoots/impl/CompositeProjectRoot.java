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

import com.gome.maven.openapi.projectRoots.ex.ProjectRoot;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.containers.ContainerUtil;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author mike
 */
class CompositeProjectRoot implements ProjectRoot {
    private final List<ProjectRoot> myRoots = new ArrayList<ProjectRoot>();

    
    ProjectRoot[] getProjectRoots() {
        return myRoots.toArray(new ProjectRoot[myRoots.size()]);
    }

    @Override
    
    public String getPresentableString() {
        throw new UnsupportedOperationException();
    }

    @Override
    
    public VirtualFile[] getVirtualFiles() {
        List<VirtualFile> result = new ArrayList<VirtualFile>();
        for (ProjectRoot root : myRoots) {
            ContainerUtil.addAll(result, root.getVirtualFiles());
        }

        return VfsUtilCore.toVirtualFileArray(result);
    }

    @Override
    
    public String[] getUrls() {
        final List<String> result = new ArrayList<String>();
        for (ProjectRoot root : myRoots) {
            ContainerUtil.addAll(result, root.getUrls());
        }
        return ArrayUtil.toStringArray(result);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    void remove( ProjectRoot root) {
        myRoots.remove(root);
    }

    
    ProjectRoot add( VirtualFile virtualFile) {
        final SimpleProjectRoot root = new SimpleProjectRoot(virtualFile);
        myRoots.add(root);
        return root;
    }

    void add( ProjectRoot root) {
        myRoots.add(root);
    }

    void remove( VirtualFile root) {
        for (Iterator<ProjectRoot> iterator = myRoots.iterator(); iterator.hasNext();) {
            ProjectRoot projectRoot = iterator.next();
            if (projectRoot instanceof SimpleProjectRoot) {
                SimpleProjectRoot r = (SimpleProjectRoot)projectRoot;
                if (root.equals(r.getFile())) {
                    iterator.remove();
                }
            }
        }
    }

    void clear() {
        myRoots.clear();
    }

    public void readExternal(Element element) throws InvalidDataException {
        final List children = element.getChildren();
        for (Object aChildren : children) {
            Element e = (Element)aChildren;
            myRoots.add(ProjectRootUtil.read(e));
        }
    }

    public void writeExternal(Element element) throws WriteExternalException {
        for (ProjectRoot root : myRoots) {
            final Element e = ProjectRootUtil.write(root);
            if (e != null) {
                element.addContent(e);
            }
        }
    }

    @Override
    public void update() {
        for (ProjectRoot root : myRoots) {
            root.update();
        }
    }

}
