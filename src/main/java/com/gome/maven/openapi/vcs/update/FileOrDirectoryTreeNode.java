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
package com.gome.maven.openapi.vcs.update;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.openapi.vcs.FileStatusManager;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointer;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointerListener;
import com.gome.maven.openapi.vfs.pointers.VirtualFilePointerManager;
import com.gome.maven.ui.SimpleTextAttributes;
import com.gome.maven.util.containers.HashMap;
import com.gome.maven.util.ui.UIUtil;

import java.awt.*;
import java.io.File;
import java.util.Map;

/**
 * author: lesya
 */
public abstract class FileOrDirectoryTreeNode extends AbstractTreeNode implements VirtualFilePointerListener, Disposable {
    private static final Map<FileStatus, SimpleTextAttributes> myFileStatusToAttributeMap = new HashMap<FileStatus, SimpleTextAttributes>();
    private final SimpleTextAttributes myInvalidAttributes;
    
    private final Project myProject;
    protected final File myFile;
    private final String myName;

    protected FileOrDirectoryTreeNode( String path,
                                       SimpleTextAttributes invalidAttributes,
                                       Project project,
                                       String parentPath) {
        String preparedPath = path.replace(File.separatorChar, '/');
        String url = VirtualFileManager.constructUrl(LocalFileSystem.getInstance().getProtocol(), preparedPath);
        setUserObject(VirtualFilePointerManager.getInstance().create(url, this, this));
        myFile = new File(getFilePath());
        myInvalidAttributes = invalidAttributes;
        myProject = project;
        myName = parentPath == null ? myFile.getAbsolutePath() : myFile.getName();
    }

    
    @Override
    public String getName() {
        return myName;
    }

    protected String getFilePath() {
        return getFilePointer().getPresentableUrl();
    }

    @Override
    public void beforeValidityChanged( VirtualFilePointer[] pointers) {
    }

    @Override
    public void validityChanged( VirtualFilePointer[] pointers) {
        if (!getFilePointer().isValid()) {
            AbstractTreeNode parent = (AbstractTreeNode)getParent();
            if (parent != null && parent.getSupportsDeletion()) {
                getTreeModel().removeNodeFromParent(this);
            }
            else {
                if (getTree() != null) {
                    getTree().repaint();
                }
            }
        }
    }

    @Override
    public void setUserObject(final Object userObject) {
        final Object oldObject = getUserObject();
        try {
            super.setUserObject(userObject);
        }
        finally {
            if (oldObject instanceof VirtualFilePointer) {
                VirtualFilePointer pointer = (VirtualFilePointer)oldObject;
                Disposer.dispose((Disposable)pointer);
            }
        }
    }

    public VirtualFilePointer getFilePointer() {
        return (VirtualFilePointer)getUserObject();
    }

    
    @Override
    public SimpleTextAttributes getAttributes() {
        if (!getFilePointer().isValid()) {
            return myInvalidAttributes;
        }
        VirtualFile file = getFilePointer().getFile();
        FileStatusManager fileStatusManager = FileStatusManager.getInstance(myProject);
        FileStatus status = fileStatusManager.getStatus(file);
        SimpleTextAttributes attributes = getAttributesFor(status);
        return myFilterAttributes == null ? attributes : SimpleTextAttributes.merge(myFilterAttributes, attributes);
    }

    
    private static SimpleTextAttributes getAttributesFor( FileStatus status) {
        Color color = status.getColor();
        if (color == null) color = UIUtil.getListForeground();

        if (!myFileStatusToAttributeMap.containsKey(status)) {
            myFileStatusToAttributeMap.put(status, new SimpleTextAttributes(Font.PLAIN, color));
        }
        return myFileStatusToAttributeMap.get(status);
    }

    @Override
    public boolean getSupportsDeletion() {
        AbstractTreeNode parent = (AbstractTreeNode)getParent();
        return parent != null && parent.getSupportsDeletion();
    }

    @Override
    public void dispose() {
    }

    
    public Project getProject() {
        return myProject;
    }
}
