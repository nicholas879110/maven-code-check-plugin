/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.gome.maven.openapi.fileTypes;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.ArrayUtil;


import java.util.Collections;
import java.util.List;

public class MockFileTypeManager extends FileTypeManager {
    @Override
    public void registerFileType( FileType type,  List<FileNameMatcher> defaultAssociations) {
        throw new UnsupportedOperationException();
    }

    
    @Override
    public FileType getFileTypeByFileName(  String fileName) {
        return MockLanguageFileType.INSTANCE;
    }

    
    @Override
    public FileType getFileTypeByFile( VirtualFile file) {
        return MockLanguageFileType.INSTANCE;
    }

    
    @Override
    public FileType getFileTypeByExtension(  String extension) {
        return MockLanguageFileType.INSTANCE;
    }

    
    @Override
    public FileType[] getRegisteredFileTypes() {
        return new FileType[] {MockLanguageFileType.INSTANCE};
    }

    @Override
    public boolean isFileIgnored(  String name) {
        return false;
    }

    @Override
    public boolean isFileIgnored(  VirtualFile file) {
        return false;
    }

    
    @Override
    public String[] getAssociatedExtensions( FileType type) {
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    
    @Override
    public List<FileNameMatcher> getAssociations( FileType type) {
        return Collections.emptyList();
    }

    @Override
    public void addFileTypeListener( FileTypeListener listener) {
    }

    @Override
    public void removeFileTypeListener( FileTypeListener listener) {
    }

    @Override
    public FileType getKnownFileTypeOrAssociate( VirtualFile file) {
        return file.getFileType();
    }

    @Override
    public FileType getKnownFileTypeOrAssociate( VirtualFile file,  Project project) {
        return getKnownFileTypeOrAssociate(file);
    }

    
    @Override
    public String getIgnoredFilesList() {
        return "";
    }

    @Override
    public void setIgnoredFilesList( String list) {
    }

    @Override
    public void associate( FileType type,  FileNameMatcher matcher) {
    }

    @Override
    public void removeAssociation( FileType type,  FileNameMatcher matcher) {
    }

    
    @Override
    public FileType getStdFileType(  String fileTypeName) {
        return MockLanguageFileType.INSTANCE;
    }

    public boolean isFileOfType( VirtualFile file,  FileType type) {
        return false;
    }

    
    @Override
    public FileType detectFileTypeFromContent( VirtualFile file) {
        return UnknownFileType.INSTANCE;
    }


    @Override
    public FileType findFileTypeByName(String fileTypeName) {
        return null;
    }
}
