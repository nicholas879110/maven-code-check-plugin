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
package com.gome.maven.core;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.roots.FileIndexFacade;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.compiled.ClsFileImpl;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yole
 */
public class CoreJavaDirectoryService extends JavaDirectoryService {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.core.CoreJavaDirectoryService");

    @Override
    public PsiPackage getPackage( PsiDirectory dir) {
        return ServiceManager.getService(dir.getProject(), CoreJavaFileManager.class).getPackage(dir);
    }

    
    @Override
    public PsiClass[] getClasses( PsiDirectory dir) {
        LOG.assertTrue(dir.isValid());

        FileIndexFacade index = FileIndexFacade.getInstance(dir.getProject());
        VirtualFile virtualDir = dir.getVirtualFile();
        boolean onlyCompiled = index.isInLibraryClasses(virtualDir) && !index.isInSourceContent(virtualDir);

        List<PsiClass> classes = null;
        for (PsiFile file : dir.getFiles()) {
            if (onlyCompiled && !(file instanceof ClsFileImpl)) {
                continue;
            }
            if (file instanceof PsiClassOwner && file.getViewProvider().getLanguages().size() == 1) {
                PsiClass[] psiClasses = ((PsiClassOwner)file).getClasses();
                if (psiClasses.length == 0) continue;
                if (classes == null) classes = new ArrayList<PsiClass>();
                ContainerUtil.addAll(classes, psiClasses);
            }
        }
        return classes == null ? PsiClass.EMPTY_ARRAY : classes.toArray(new PsiClass[classes.size()]);
    }

    
    @Override
    public PsiClass createClass( PsiDirectory dir,  String name) throws IncorrectOperationException {
        throw new UnsupportedOperationException();
    }

    
    @Override
    public PsiClass createClass( PsiDirectory dir,  String name,  String templateName)
            throws IncorrectOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PsiClass createClass( PsiDirectory dir,
                                 String name,
                                 String templateName,
                                boolean askForUndefinedVariables) throws IncorrectOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PsiClass createClass( PsiDirectory dir,
                                 String name,
                                 String templateName,
                                boolean askForUndefinedVariables,  final Map<String, String> additionalProperties) throws IncorrectOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkCreateClass( PsiDirectory dir,  String name) throws IncorrectOperationException {
        throw new UnsupportedOperationException();
    }

    
    @Override
    public PsiClass createInterface( PsiDirectory dir,  String name) throws IncorrectOperationException {
        throw new UnsupportedOperationException();
    }

    
    @Override
    public PsiClass createEnum( PsiDirectory dir,  String name) throws IncorrectOperationException {
        throw new UnsupportedOperationException();
    }

    
    @Override
    public PsiClass createAnnotationType( PsiDirectory dir,  String name) throws IncorrectOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSourceRoot( PsiDirectory dir) {
        return false;
    }

    @Override
    public LanguageLevel getLanguageLevel( PsiDirectory dir) {
        return LanguageLevel.HIGHEST;
    }
}
