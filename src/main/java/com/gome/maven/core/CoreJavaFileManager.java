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
package com.gome.maven.core;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.file.PsiPackageImpl;
import com.gome.maven.psi.impl.file.impl.JavaFileManager;
import com.gome.maven.psi.search.GlobalSearchScope;

import java.util.*;

/**
 * @author yole
 */
public class CoreJavaFileManager implements JavaFileManager {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.core.CoreJavaFileManager");

    private final List<VirtualFile> myClasspath = new ArrayList<VirtualFile>();

    private final PsiManager myPsiManager;

    public CoreJavaFileManager(PsiManager psiManager) {
        myPsiManager = psiManager;
    }

    private List<VirtualFile> roots() {
        return myClasspath;
    }

    @Override
    public PsiPackage findPackage( String packageName) {
        final List<VirtualFile> files = findDirectoriesByPackageName(packageName);
        if (!files.isEmpty()) {
            return new PsiPackageImpl(myPsiManager, packageName);
        }
        return null;
    }

    private List<VirtualFile> findDirectoriesByPackageName(String packageName) {
        List<VirtualFile> result = new ArrayList<VirtualFile>();
        String dirName = packageName.replace(".", "/");
        for (VirtualFile root : roots()) {
            VirtualFile classDir = root.findFileByRelativePath(dirName);
            if (classDir != null) {
                result.add(classDir);
            }
        }
        return result;
    }

    
    public PsiPackage getPackage(PsiDirectory dir) {
        final VirtualFile file = dir.getVirtualFile();
        for (VirtualFile root : myClasspath) {
            if (VfsUtilCore.isAncestor(root, file, false)) {
                String relativePath = FileUtil.getRelativePath(root.getPath(), file.getPath(), '/');
                if (relativePath == null) continue;
                return new PsiPackageImpl(myPsiManager, relativePath.replace('/', '.'));
            }
        }
        return null;
    }

    @Override
    public PsiClass findClass( String qName,  GlobalSearchScope scope) {
        for (VirtualFile root : roots()) {
            final PsiClass psiClass = findClassInClasspathRoot(qName, root, myPsiManager, scope);
            if (psiClass != null) {
                return psiClass;
            }
        }
        return null;
    }

    
    public static PsiClass findClassInClasspathRoot( String qName,
                                                     VirtualFile root,
                                                     PsiManager psiManager,
                                                     GlobalSearchScope scope) {
        String pathRest = qName;
        VirtualFile cur = root;

        while (true) {
            int dot = pathRest.indexOf('.');
            if (dot < 0) break;

            String pathComponent = pathRest.substring(0, dot);
            VirtualFile child = cur.findChild(pathComponent);

            if (child == null) break;
            pathRest = pathRest.substring(dot + 1);
            cur = child;
        }

        String classNameWithInnerClasses = pathRest;
        String topLevelClassName = substringBeforeFirstDot(classNameWithInnerClasses);

        VirtualFile vFile = cur.findChild(topLevelClassName + ".class");
        if (vFile == null) vFile = cur.findChild(topLevelClassName + ".java");

        if (vFile == null) {
            return null;
        }
        if (!vFile.isValid()) {
            LOG.error("Invalid child of valid parent: " + vFile.getPath() + "; " + root.isValid() + " path=" + root.getPath());
            return null;
        }
        if (!scope.contains(vFile)) {
            return null;
        }

        final PsiFile file = psiManager.findFile(vFile);
        if (!(file instanceof PsiClassOwner)) {
            return null;
        }

        return findClassInPsiFile(classNameWithInnerClasses, (PsiClassOwner)file);
    }

    
    private static String substringBeforeFirstDot( String classNameWithInnerClasses) {
        int dot = classNameWithInnerClasses.indexOf('.');
        if (dot < 0) {
            return classNameWithInnerClasses;
        }
        else {
            return classNameWithInnerClasses.substring(0, dot);
        }
    }

    
    private static PsiClass findClassInPsiFile( String classNameWithInnerClassesDotSeparated,  PsiClassOwner file) {
        for (PsiClass topLevelClass : file.getClasses()) {
            PsiClass candidate = findClassByTopLevelClass(classNameWithInnerClassesDotSeparated, topLevelClass);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    
    private static PsiClass findClassByTopLevelClass( String className,  PsiClass topLevelClass) {
        if (className.indexOf('.') < 0) {
            return className.equals(topLevelClass.getName()) ? topLevelClass : null;
        }

        Iterator<String> segments = StringUtil.split(className, ".").iterator();
        if (!segments.hasNext() || !segments.next().equals(topLevelClass.getName())) {
            return null;
        }
        PsiClass curClass = topLevelClass;
        while (segments.hasNext()) {
            String innerClassName = segments.next();
            PsiClass innerClass = curClass.findInnerClassByName(innerClassName, false);
            if (innerClass == null) {
                return null;
            }
            curClass = innerClass;
        }
        return curClass;
    }

    
    @Override
    public PsiClass[] findClasses( String qName,  GlobalSearchScope scope) {
        List<PsiClass> result = new ArrayList<PsiClass>();
        for (VirtualFile file : roots()) {
            final PsiClass psiClass = findClassInClasspathRoot(qName, file, myPsiManager, scope);
            if (psiClass != null) {
                result.add(psiClass);
            }
        }
        return result.toArray(new PsiClass[result.size()]);
    }

    
    @Override
    public Collection<String> getNonTrivialPackagePrefixes() {
        return Collections.emptyList();
    }

    public void addToClasspath(VirtualFile root) {
        myClasspath.add(root);
    }
}
