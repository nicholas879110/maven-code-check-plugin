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
package com.gome.maven.refactoring;

import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.JavaPsiFacade;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.PsiPackage;

/**
 * Represents a package. 
 *  @author dsl
 */
public class PackageWrapper {
    private final PsiManager myManager;
     private final String myQualifiedName;

    public PackageWrapper(PsiManager manager,  String qualifiedName) {
        myManager = manager;
        myQualifiedName = qualifiedName;
    }

    public PackageWrapper(PsiPackage aPackage) {
        myManager = aPackage.getManager();
        myQualifiedName = aPackage.getQualifiedName();
    }

    public PsiManager getManager() { return myManager; }

    public PsiDirectory[] getDirectories() {
        String qName = myQualifiedName;
        while (qName.endsWith(".")) {
            qName = StringUtil.trimEnd(qName, ".");
        }
        final PsiPackage aPackage = JavaPsiFacade.getInstance(myManager.getProject()).findPackage(qName);
        if (aPackage != null) {
            return aPackage.getDirectories();
        } else {
            return PsiDirectory.EMPTY_ARRAY;
        }
    }

    public boolean exists() {
        return JavaPsiFacade.getInstance(myManager.getProject()).findPackage(myQualifiedName) != null;
    }

    
    public String getQualifiedName() {
        return myQualifiedName;
    }

    public boolean equalToPackage(PsiPackage aPackage) {
        return aPackage != null && myQualifiedName.equals(aPackage.getQualifiedName());
    }

    public static PackageWrapper create(PsiPackage aPackage) {
        return new PackageWrapper(aPackage);
    }
}
