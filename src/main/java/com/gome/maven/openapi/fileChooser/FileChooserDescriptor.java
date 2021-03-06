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
package com.gome.maven.openapi.fileChooser;

import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.fileTypes.FileTypeManager;
import com.gome.maven.openapi.fileTypes.FileTypes;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.openapi.vfs.JarFileSystem;
import com.gome.maven.openapi.vfs.VFileProperty;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.LayeredIcon;
import com.gome.maven.ui.UIBundle;
import com.gome.maven.util.IconUtil;
import com.gome.maven.util.PlatformIcons;

import javax.swing.*;
import java.util.*;

/**
 * @see FileChooserDescriptorFactory
 */
public class FileChooserDescriptor implements Cloneable {
    private final boolean myChooseFiles;
    private final boolean myChooseFolders;
    private final boolean myChooseJars;
    private final boolean myChooseJarsAsFiles;
    private final boolean myChooseJarContents;
    private final boolean myChooseMultiple;

    private String myTitle = UIBundle.message("file.chooser.default.title");
    private String myDescription;

    private boolean myHideIgnored = true;
    private final List<VirtualFile> myRoots = new ArrayList<VirtualFile>();
    private boolean myShowFileSystemRoots = true;
    private boolean myTreeRootVisible = false;
    private boolean myShowHiddenFiles = false;
    private Condition<VirtualFile> myFileFilter = null;

    private final Map<String, Object> myUserData = new HashMap<String, Object>();

    /**
     * Creates new instance. Use methods from {@link FileChooserDescriptorFactory} for most used descriptors.
     *
     * @param chooseFiles       controls whether files can be chosen
     * @param chooseFolders     controls whether folders can be chosen
     * @param chooseJars        controls whether .jar files can be chosen
     * @param chooseJarsAsFiles controls whether .jar files will be returned as files or as folders
     * @param chooseJarContents controls whether .jar file contents can be chosen
     * @param chooseMultiple    controls how many files can be chosen
     */
    public FileChooserDescriptor(boolean chooseFiles,
                                 boolean chooseFolders,
                                 boolean chooseJars,
                                 boolean chooseJarsAsFiles,
                                 boolean chooseJarContents,
                                 boolean chooseMultiple) {
        myChooseFiles = chooseFiles;
        myChooseFolders = chooseFolders;
        myChooseJars = chooseJars;
        myChooseJarsAsFiles = chooseJarsAsFiles;
        myChooseJarContents = chooseJarContents;
        myChooseMultiple = chooseMultiple;
    }

    public FileChooserDescriptor( FileChooserDescriptor d) {
        this(d.isChooseFiles(), d.isChooseFolders(), d.isChooseJars(), d.isChooseJarsAsFiles(), d.isChooseJarContents(), d.isChooseMultiple());
        withTitle(d.getTitle());
        withDescription(d.getDescription());
        withHideIgnored(d.isHideIgnored());
        withRoots(d.getRoots());
        withShowFileSystemRoots(d.isShowFileSystemRoots());
        withTreeRootVisible(d.isTreeRootVisible());
        withShowHiddenFiles(d.isShowHiddenFiles());
    }

    public boolean isChooseFiles() {
        return myChooseFiles;
    }

    public boolean isChooseFolders() {
        return myChooseFolders;
    }

    public boolean isChooseJars() {
        return myChooseJars;
    }

    public boolean isChooseJarsAsFiles() {
        return myChooseJarsAsFiles;
    }

    public boolean isChooseJarContents() {
        return myChooseJarContents;
    }

    public boolean isChooseMultiple() {
        return myChooseMultiple;
    }

    /** @deprecated use {@link #isChooseMultiple()} (to be removed in IDEA 15) */
    @SuppressWarnings("UnusedDeclaration")
    public boolean getChooseMultiple() {
        return isChooseMultiple();
    }

    public String getTitle() {
        return myTitle;
    }

    public void setTitle(String title) {
        withTitle(title);
    }

    public FileChooserDescriptor withTitle(String title) {
        myTitle = title;
        return this;
    }

    public String getDescription() {
        return myDescription;
    }

    public void setDescription(String description) {
        withDescription(description);
    }

    public FileChooserDescriptor withDescription(String description) {
        myDescription = description;
        return this;
    }

    public boolean isHideIgnored() {
        return myHideIgnored;
    }

    public void setHideIgnored(boolean hideIgnored) {
        withHideIgnored(hideIgnored);
    }

    public FileChooserDescriptor withHideIgnored(boolean hideIgnored) {
        myHideIgnored = hideIgnored;
        return this;
    }

    public List<VirtualFile> getRoots() {
        return Collections.unmodifiableList(myRoots);
    }

    public void setRoots( VirtualFile... roots) {
        withRoots(roots);
    }

    public void setRoots( List<VirtualFile> roots) {
        withRoots(roots);
    }

    public FileChooserDescriptor withRoots(final VirtualFile... roots) {
        return withRoots(Arrays.asList(roots));
    }

    public FileChooserDescriptor withRoots( List<VirtualFile> roots) {
        myRoots.clear();
        myRoots.addAll(roots);
        return this;
    }

    public boolean isShowFileSystemRoots() {
        return myShowFileSystemRoots;
    }

    public void setShowFileSystemRoots(boolean showFileSystemRoots) {
        withShowFileSystemRoots(showFileSystemRoots);
    }

    public FileChooserDescriptor withShowFileSystemRoots(boolean showFileSystemRoots) {
        myShowFileSystemRoots = showFileSystemRoots;
        return this;
    }

    public boolean isTreeRootVisible() {
        return myTreeRootVisible;
    }

    public FileChooserDescriptor withTreeRootVisible(boolean isTreeRootVisible) {
        myTreeRootVisible = isTreeRootVisible;
        return this;
    }

    /** @deprecated use {@link #withTreeRootVisible(boolean)} (to be removed in IDEA 15) */
    @SuppressWarnings("UnusedDeclaration")
    public FileChooserDescriptor setIsTreeRootVisible(boolean treeRootVisible) {
        return withTreeRootVisible(treeRootVisible);
    }

    public boolean isShowHiddenFiles() {
        return myShowHiddenFiles;
    }

    public FileChooserDescriptor withShowHiddenFiles(boolean showHiddenFiles) {
        myShowHiddenFiles = showHiddenFiles;
        return this;
    }

    /**
     * Sets simple boolean condition for use in {@link #isFileVisible(VirtualFile, boolean)} and {@link #isFileSelectable(VirtualFile)}.
     */
    public FileChooserDescriptor withFileFilter( Condition<VirtualFile> filter) {
        myFileFilter = filter;
        return this;
    }

    /**
     * Defines whether a file is visible in the tree.
     */
    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        if (file.is(VFileProperty.SYMLINK) && file.getCanonicalPath() == null) {
            return false;
        }

        if (!file.isDirectory()) {
            if (FileElement.isArchive(file)) {
                if (!myChooseJars && !myChooseJarContents) {
                    return false;
                }
            }
            else if (!myChooseFiles) {
                return false;
            }
            if (myFileFilter != null && !myFileFilter.value(file)) {
                return false;
            }
        }

        if (isHideIgnored() && FileTypeManager.getInstance().isFileIgnored(file)) {
            return false;
        }

        if (!showHiddenFiles && FileElement.isFileHidden(file)) {
            return false;
        }

        return true;
    }

    /**
     * Defines whether a file can be chosen.
     */
    public boolean isFileSelectable(VirtualFile file) {
        if (file == null) return false;

        if (file.is(VFileProperty.SYMLINK) && file.getCanonicalPath() == null) {
            return false;
        }
        if (file.isDirectory() && myChooseFolders) {
            return true;
        }
        if (acceptAsJarFile(file)) {
            return true;
        }
        if (acceptAsGeneralFile(file)) {
            return true;
        }
        if (myFileFilter != null && !file.isDirectory() && myFileFilter.value(file)) {
            return true;
        }

        return false;
    }

    public Icon getIcon(final VirtualFile file) {
        if (file.isDirectory()) {
            return dressIcon(file, PlatformIcons.DIRECTORY_CLOSED_ICON);
        }
        return IconUtil.getIcon(file, Iconable.ICON_FLAG_READ_STATUS, null);
    }

    protected static Icon dressIcon(final VirtualFile file, final Icon baseIcon) {
        return file.isValid() && file.is(VFileProperty.SYMLINK) ? new LayeredIcon(baseIcon, PlatformIcons.SYMLINK_ICON) : baseIcon;
    }

    public String getName(final VirtualFile file) {
        return file.getPath();
    }

    
    public String getComment(final VirtualFile file) {
        return null;
    }

    /**
     * the method is called upon pressing Ok in the FileChooserDialog
     * Override the method in order to customize validation of user input
     * @param files - selected files to be checked
     * @throws Exception if the the files cannot be accepted
     */
    public void validateSelectedFiles(VirtualFile[] files) throws Exception {
    }

    private boolean acceptAsGeneralFile(VirtualFile file) {
        if (FileElement.isArchive(file)) return false; // should be handle by acceptsAsJarFile
        return !file.isDirectory() && myChooseFiles;
    }

    private boolean acceptAsJarFile(VirtualFile file) {
        return myChooseJars && FileElement.isArchive(file);
    }

    
    public final VirtualFile getFileToSelect(VirtualFile file) {
        if (file.isDirectory() && (myChooseFolders || isFileSelectable(file))) {
            return file;
        }
        boolean isJar = file.getFileType() == FileTypes.ARCHIVE;
        if (!isJar) {
            return acceptAsGeneralFile(file) ? file : null;
        }
        if (myChooseJarsAsFiles) {
            return file;
        }
        if (!acceptAsJarFile(file)) {
            return null;
        }
        String path = file.getPath();
        return JarFileSystem.getInstance().findFileByPath(path + JarFileSystem.JAR_SEPARATOR);
    }

    @Override
    public final Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    
    public Object getUserData(String dataId) {
        return myUserData.get(dataId);
    }

    
    public <T> T getUserData( DataKey<T> key) {
        @SuppressWarnings({"unchecked"}) final T t = (T)myUserData.get(key.getName());
        return t;
    }

    public <T> void putUserData( DataKey<T> key,  T data) {
        myUserData.put(key.getName(), data);
    }

    @Override
    public String toString() {
        return "FileChooserDescriptor [" + myTitle + "]";
    }
}
