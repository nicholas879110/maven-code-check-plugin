/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.ide.highlighter;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.lang.java.JavaLanguage;
import com.gome.maven.openapi.fileTypes.LanguageFileType;

import javax.swing.*;

public class JavaFileType extends LanguageFileType {
     public static final String DEFAULT_EXTENSION = "java";
     public static final String DOT_DEFAULT_EXTENSION = ".java";
    public static final JavaFileType INSTANCE = new JavaFileType();

    private JavaFileType() {
        super(JavaLanguage.INSTANCE);
    }

    @Override
    
    public String getName() {
        return "JAVA";
    }

    @Override
    
    public String getDescription() {
        return IdeBundle.message("filetype.description.java");
    }

    @Override
    
    public String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    @Override
    public Icon getIcon() {
        return AllIcons.FileTypes.Java;
    }

    @Override
    public boolean isJVMDebuggingSupported() {
        return true;
    }
}
