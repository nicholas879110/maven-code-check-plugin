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

import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.fileTypes.PlainTextFileType;
import com.gome.maven.openapi.fileTypes.PlainTextLanguage;
import com.gome.maven.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.gome.maven.openapi.vfs.VirtualFile;

import javax.swing.*;

/**
 * @author gregsh
 */
public class ScratchFileType extends LanguageFileType implements FileTypeIdentifiableByVirtualFile {

    public static final LanguageFileType INSTANCE = new ScratchFileType();

    ScratchFileType() {
        super(PlainTextLanguage.INSTANCE);
    }

    @Override
    public boolean isMyFileType( VirtualFile file) {
        return ScratchFileService.getInstance().getRootType(file) != null;
    }

    
    @Override
    public String getName() {
        return "Scratch";
    }

    
    @Override
    public String getDescription() {
        return "Scratch";
    }

    
    @Override
    public String getDefaultExtension() {
        return "";
    }

    
    @Override
    public Icon getIcon() {
        return PlainTextFileType.INSTANCE.getIcon();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    
    @Override
    public String getCharset( VirtualFile file,  byte[] content) {
        return null;
    }
}
