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
package com.gome.maven.psi;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.VirtualFile;

/**
 * @author max
 */
public abstract class PsiFileFactory {
    public static Key<PsiFile> ORIGINAL_FILE = Key.create("ORIGINAL_FILE");

    public static PsiFileFactory getInstance(Project project) {
        return ServiceManager.getService(project, PsiFileFactory.class);
    }

    /**
     * Please use {@link #createFileFromText(String, com.gome.maven.openapi.fileTypes.FileType, CharSequence)},
     * since file type detecting by file extension becomes vulnerable when file type mappings are changed.
     * <p/>
     * Creates a file from the specified text.
     *
     * @param name the name of the file to create (the extension of the name determines the file type).
     * @param text the text of the file to create.
     * @return the created file.
     * @throws com.gome.maven.util.IncorrectOperationException
     *          if the file type with specified extension is binary.
     */
    @Deprecated
    
    public abstract PsiFile createFileFromText(  String name,   String text);

    
    public abstract PsiFile createFileFromText(  String fileName,  FileType fileType,  CharSequence text);

    
    public abstract PsiFile createFileFromText(  String name,  FileType fileType,  CharSequence text,
                                               long modificationStamp, boolean eventSystemEnabled);

    
    public abstract PsiFile createFileFromText(  String name,  FileType fileType,  CharSequence text,
                                               long modificationStamp, boolean eventSystemEnabled, boolean markAsCopy);

    public abstract PsiFile createFileFromText( String name,  Language language,  CharSequence text);

    public PsiFile createFileFromText( Language language,  CharSequence text) {
        return createFileFromText("foo.bar", language, text);
    }

    public abstract PsiFile createFileFromText( String name,  Language language,  CharSequence text,
                                               boolean eventSystemEnabled, boolean markAsCopy);

    public abstract PsiFile createFileFromText( String name,  Language language,  CharSequence text,
                                               boolean eventSystemEnabled, boolean markAsCopy, boolean noSizeLimit);

    public abstract PsiFile createFileFromText( String name,  Language language,  CharSequence text,
                                               boolean eventSystemEnabled, boolean markAsCopy, boolean noSizeLimit,
                                                VirtualFile original);

    public abstract PsiFile createFileFromText(FileType fileType, String fileName, CharSequence chars, int startOffset, int endOffset);

    
    public abstract PsiFile createFileFromText( CharSequence chars,  PsiFile original);
}