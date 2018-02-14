/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package com.gome.maven.ide.fileTemplates;

import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.CharsetToolkit;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * @author MYakovlev
 * Date: Jul 24, 2002
 */
public interface FileTemplate extends Cloneable {
    FileTemplate[] EMPTY_ARRAY = new FileTemplate[0];

    String ATTRIBUTE_EXCEPTION = "EXCEPTION";
    String ATTRIBUTE_EXCEPTION_TYPE = "EXCEPTION_TYPE";
    String ATTRIBUTE_DESCRIPTION = "DESCRIPTION";
    String ATTRIBUTE_DISPLAY_NAME = "DISPLAY_NAME";

    String ATTRIBUTE_RETURN_TYPE = "RETURN_TYPE";
    String ATTRIBUTE_DEFAULT_RETURN_VALUE = "DEFAULT_RETURN_VALUE";
    String ATTRIBUTE_CALL_SUPER = "CALL_SUPER";

    String ourEncoding = CharsetToolkit.UTF8;
    String ATTRIBUTE_CLASS_NAME = "CLASS_NAME";
    String ATTRIBUTE_SIMPLE_CLASS_NAME = "SIMPLE_CLASS_NAME";
    String ATTRIBUTE_METHOD_NAME = "METHOD_NAME";
    String ATTRIBUTE_PACKAGE_NAME = "PACKAGE_NAME";
    String ATTRIBUTE_NAME = "NAME";
    String ATTRIBUTE_FILE_NAME = "FILE_NAME";

    /** Name without extension */
     String getName();

    void setName( String name);

    boolean isTemplateOfType(final FileType fType);

    boolean isDefault();

    
    String getDescription();

    
    String getText();

    void setText(String text);

    
    String getText(Map attributes) throws IOException;

    
    String getText(Properties attributes) throws IOException;

     String getExtension();

    void setExtension( String extension);

    boolean isReformatCode();

    void setReformatCode(boolean reformat);

    FileTemplate clone();

     String[] getUnsetAttributes( Properties properties, Project project) throws Exception;
}
