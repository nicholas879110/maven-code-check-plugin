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

package com.gome.maven.util.descriptors;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.util.ModificationTracker;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.xml.XmlFile;

/**
 * @author nik
 */
public interface ConfigFile extends Disposable, ModificationTracker {
    ConfigFile[] EMPTY_ARRAY = new ConfigFile[0];

    String getUrl();

    
    VirtualFile getVirtualFile();

    
    PsiFile getPsiFile();

    
    XmlFile getXmlFile();


    
    ConfigFileMetaData getMetaData();

    
    ConfigFileInfo getInfo();

    boolean isValid();
}
