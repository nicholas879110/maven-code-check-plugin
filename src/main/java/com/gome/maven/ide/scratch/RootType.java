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
package com.gome.maven.ide.scratch;

import com.gome.maven.ide.util.treeView.AbstractTreeBuilder;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypeManager;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.LanguageSubstitutors;

import javax.swing.*;
import java.io.IOException;

/**
 * @author gregsh
 *
 * Created on 1/19/15
 */
public abstract class RootType {

    public static final ExtensionPointName<RootType> ROOT_EP = ExtensionPointName.create("com.intellij.scratch.rootType");

    
    public static RootType[] getAllRootIds() {
        return Extensions.getExtensions(ROOT_EP);
    }

    
    public static RootType findById( String id) {
        for (RootType type : getAllRootIds()) {
            if (id.equals(type.getId())) return type;
        }
        throw new AssertionError(id);
    }

    
    public static <T extends RootType> T findByClass(Class<T> aClass) {
        return Extensions.findExtension(ROOT_EP, aClass);
    }

    private final String myId;
    private final String myDisplayName;

    protected RootType( String id,  String displayName) {
        myId = id;
        myDisplayName = displayName;
    }

    
    public final String getId() {
        return myId;
    }

    
    public final String getDisplayName() {
        return myDisplayName;
    }

    public boolean isHidden() {
        return StringUtil.isEmpty(myDisplayName);
    }

    
    public Language substituteLanguage( Project project,  VirtualFile file) {
        return substituteLanguageImpl(getOriginalLanguage(file), file, project);
    }

    
    public Icon substituteIcon( Project project,  VirtualFile file) {
        Language language = substituteLanguage(project, file);
        LanguageFileType fileType = language != null ? language.getAssociatedFileType() : null;
        return fileType != null ? fileType.getIcon() : null;
    }

    
    public String substituteName( Project project,  VirtualFile file) {
        return null;
    }

    public VirtualFile findFile( Project project,  String pathName, ScratchFileService.Option option) throws IOException {
        return ScratchFileService.getInstance().findFile(this, pathName, option);
    }

    public void fileOpened( VirtualFile file,  FileEditorManager source) {
    }

    
    protected static Language substituteLanguageImpl(Language language, VirtualFile file, Project project) {
        return language != null && language != ScratchFileType.INSTANCE.getLanguage() ?
                LanguageSubstitutors.INSTANCE.substituteLanguage(language, file, project) : language;
    }

    
    protected static FileType getOriginalFileType( VirtualFile file) {
        String extension = file.getExtension();
        if (extension == null) return null;
        return FileTypeManager.getInstance().getFileTypeByExtension(extension);
    }

    
    protected static Language getOriginalLanguage( VirtualFile file) {
        FileType fileType = getOriginalFileType(file);
        return fileType instanceof LanguageFileType ? ((LanguageFileType)fileType).getLanguage() : null;
    }

    public boolean isIgnored( Project project,  VirtualFile element) {
        return false;
    }

    public void registerTreeUpdater( Project project,  AbstractTreeBuilder builder) {
    }

}
