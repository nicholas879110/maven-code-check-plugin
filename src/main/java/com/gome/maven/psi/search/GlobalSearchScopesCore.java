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
package com.gome.maven.psi.search;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiBundle;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.search.scope.packageSet.*;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.Function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class GlobalSearchScopesCore {
    
    public static GlobalSearchScope projectProductionScope( Project project) {
        return new ProductionScopeFilter(project);
    }

    
    public static GlobalSearchScope projectTestScope( Project project) {
        return new TestScopeFilter(project);
    }

    
    public static GlobalSearchScope directoryScope( PsiDirectory directory, final boolean withSubdirectories) {
        return new DirectoryScope(directory, withSubdirectories);
    }

    
    public static GlobalSearchScope directoryScope( Project project,  VirtualFile directory, final boolean withSubdirectories) {
        return new DirectoryScope(project, directory, withSubdirectories);
    }

    
    public static GlobalSearchScope directoriesScope( Project project, boolean withSubdirectories,  VirtualFile... directories) {
        if (directories.length ==1) {
            return directoryScope(project, directories[0], withSubdirectories);
        }
        BitSet withSubdirectoriesBS = new BitSet(directories.length);
        if (withSubdirectories) {
            withSubdirectoriesBS.set(0, directories.length-1);
        }
        return new DirectoriesScope(project, directories, withSubdirectoriesBS);
    }

    public static GlobalSearchScope filterScope( Project project,  NamedScope set) {
        return new FilterScopeAdapter(project, set);
    }

    private static class FilterScopeAdapter extends GlobalSearchScope {
        private final NamedScope mySet;
        private final PsiManager myManager;

        private FilterScopeAdapter( Project project,  NamedScope set) {
            super(project);
            mySet = set;
            myManager = PsiManager.getInstance(project);
        }

        @Override
        public boolean contains( VirtualFile file) {
            Project project = getProject();
            NamedScopesHolder holder = NamedScopeManager.getInstance(project);
            final PackageSet packageSet = mySet.getValue();
            if (packageSet != null) {
                if (packageSet instanceof PackageSetBase) return ((PackageSetBase)packageSet).contains(file, project, holder);
                PsiFile psiFile = myManager.findFile(file);
                return psiFile != null && packageSet.contains(psiFile, holder);
            }
            return false;
        }

        
        @Override
        public String getDisplayName() {
            return mySet.getName();
        }

        
        @Override
        public Project getProject() {
            return super.getProject();
        }

        @Override
        public int compare( VirtualFile file1,  VirtualFile file2) {
            return 0;

        }

        @Override
        public boolean isSearchInModuleContent( Module aModule) {
            return true; //TODO (optimization?)
        }

        @Override
        public boolean isSearchInLibraries() {
            return true; //TODO (optimization?)
        }
    }

    private static class ProductionScopeFilter extends GlobalSearchScope {
        private final ProjectFileIndex myFileIndex;

        private ProductionScopeFilter( Project project) {
            super(project);
            myFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        }

        @Override
        public boolean contains( VirtualFile file) {
            return myFileIndex.isInSourceContent(file) && !myFileIndex.isInTestSourceContent(file);
        }

        @Override
        public int compare( VirtualFile file1,  VirtualFile file2) {
            return 0;
        }

        @Override
        public boolean isSearchInModuleContent( Module aModule) {
            return true;
        }

        @Override
        public boolean isSearchInModuleContent( final Module aModule, final boolean testSources) {
            return !testSources;
        }

        @Override
        public boolean isSearchInLibraries() {
            return false;
        }

        
        @Override
        public String getDisplayName() {
            return PsiBundle.message("psi.search.scope.production.files");
        }
    }

    private static class TestScopeFilter extends GlobalSearchScope {
        private final ProjectFileIndex myFileIndex;

        private TestScopeFilter( Project project) {
            super(project);
            myFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        }

        @Override
        public boolean contains( VirtualFile file) {
            return myFileIndex.isInTestSourceContent(file);
        }

        @Override
        public int compare( VirtualFile file1,  VirtualFile file2) {
            return 0;
        }

        @Override
        public boolean isSearchInModuleContent( Module aModule) {
            return true;
        }

        @Override
        public boolean isSearchInModuleContent( final Module aModule, final boolean testSources) {
            return testSources;
        }

        @Override
        public boolean isSearchInLibraries() {
            return false;
        }

        
        @Override
        public String getDisplayName() {
            return PsiBundle.message("psi.search.scope.test.files");
        }
    }

    private static class DirectoryScope extends GlobalSearchScope {
        private final VirtualFile myDirectory;
        private final boolean myWithSubdirectories;

        private DirectoryScope( PsiDirectory psiDirectory, final boolean withSubdirectories) {
            super(psiDirectory.getProject());
            myWithSubdirectories = withSubdirectories;
            myDirectory = psiDirectory.getVirtualFile();
        }

        private DirectoryScope( Project project,  VirtualFile directory, final boolean withSubdirectories) {
            super(project);
            myWithSubdirectories = withSubdirectories;
            myDirectory = directory;
        }

        @Override
        public boolean contains( VirtualFile file) {
            return myWithSubdirectories ? VfsUtilCore.isAncestor(myDirectory, file, false) : myDirectory.equals(file.getParent());
        }

        @Override
        public int compare( VirtualFile file1,  VirtualFile file2) {
            return 0;
        }

        @Override
        public boolean isSearchInModuleContent( Module aModule) {
            return true;
        }

        @Override
        public boolean isSearchInLibraries() {
            return false;
        }

        @Override
        public String toString() {
            //noinspection HardCodedStringLiteral
            return "directory scope: " + myDirectory + "; withSubdirs:"+myWithSubdirectories;
        }

        @Override
        public int hashCode() {
            return myDirectory.hashCode() *31 + (myWithSubdirectories?1:0);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DirectoryScope &&
                    myDirectory.equals(((DirectoryScope)obj).myDirectory) &&
                    myWithSubdirectories == ((DirectoryScope)obj).myWithSubdirectories;
        }

        
        @Override
        public GlobalSearchScope uniteWith( GlobalSearchScope scope) {
            if (equals(scope)) return this;
            if (scope instanceof DirectoryScope) {
                DirectoryScope other = (DirectoryScope)scope;
                VirtualFile otherDirectory = other.myDirectory;
                if (myWithSubdirectories && VfsUtilCore.isAncestor(myDirectory, otherDirectory, false)) return this;
                if (other.myWithSubdirectories && VfsUtilCore.isAncestor(otherDirectory, myDirectory, false)) return other;
                BitSet newWithSubdirectories = new BitSet();
                newWithSubdirectories.set(0, myWithSubdirectories);
                newWithSubdirectories.set(1, other.myWithSubdirectories);
                return new DirectoriesScope(getProject(), new VirtualFile[]{myDirectory,otherDirectory}, newWithSubdirectories);
            }
            return super.uniteWith(scope);
        }

        
        @Override
        public Project getProject() {
            return super.getProject();
        }

        
        @Override
        public String getDisplayName() {
            return "Directory '" + myDirectory.getName() + "'";
        }
    }

    static class DirectoriesScope extends GlobalSearchScope {
        private final VirtualFile[] myDirectories;
        private final BitSet myWithSubdirectories;

        private DirectoriesScope( Project project,  VirtualFile[] directories,  BitSet withSubdirectories) {
            super(project);
            myWithSubdirectories = withSubdirectories;
            myDirectories = directories;
            if (directories.length < 2) {
                throw new IllegalArgumentException("Expected >1 directories, but got: " + Arrays.asList(directories));
            }
        }

        @Override
        public boolean contains( VirtualFile file) {
            VirtualFile parent = file.getParent();
            return parent != null && in(parent);
        }

        private boolean in( VirtualFile parent) {
            for (int i = 0; i < myDirectories.length; i++) {
                VirtualFile directory = myDirectories[i];
                boolean withSubdirectories = myWithSubdirectories.get(i);
                if (withSubdirectories ? VfsUtilCore.isAncestor(directory, parent, false) : directory.equals(parent)) return true;
            }
            return false;
        }

        @Override
        public int compare( VirtualFile file1,  VirtualFile file2) {
            return 0;
        }

        @Override
        public boolean isSearchInModuleContent( Module aModule) {
            return true;
        }

        @Override
        public boolean isSearchInLibraries() {
            return false;
        }

        @Override
        public String toString() {
            //noinspection HardCodedStringLiteral
            return "Directories scope: " + Arrays.asList(myDirectories);
        }

        @Override
        public int hashCode() {
            int result = 1;
            for (int i = 0; i < myDirectories.length; i++) {
                VirtualFile directory = myDirectories[i];
                boolean withSubdirectories = myWithSubdirectories.get(i);
                result = result*31 + directory.hashCode() *31 + (withSubdirectories?1:0);
            }
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DirectoriesScope &&
                    Arrays.equals(myDirectories, ((DirectoriesScope)obj).myDirectories) &&
                    myWithSubdirectories.equals(((DirectoriesScope)obj).myWithSubdirectories);
        }

        
        @Override
        public GlobalSearchScope uniteWith( GlobalSearchScope scope) {
            if (equals(scope)) {
                return this;
            }
            if (scope instanceof DirectoryScope) {
                if (in(((DirectoryScope)scope).myDirectory)) {
                    return this;
                }
                VirtualFile[] newDirectories = ArrayUtil.append(myDirectories, ((DirectoryScope)scope).myDirectory, VirtualFile.class);
                BitSet newWithSubdirectories = (BitSet)myWithSubdirectories.clone();
                newWithSubdirectories.set(myDirectories.length, ((DirectoryScope)scope).myWithSubdirectories);
                return new DirectoriesScope(getProject(), newDirectories, newWithSubdirectories);
            }
            if (scope instanceof DirectoriesScope) {
                DirectoriesScope other = (DirectoriesScope)scope;
                List<VirtualFile> newDirectories = new ArrayList<VirtualFile>(myDirectories.length + other.myDirectories.length);
                newDirectories.addAll(Arrays.asList(other.myDirectories));
                BitSet newWithSubdirectories = (BitSet)myWithSubdirectories.clone();
                VirtualFile[] directories = other.myDirectories;
                for (int i = 0; i < directories.length; i++) {
                    VirtualFile otherDirectory = directories[i];
                    if (!in(otherDirectory)) {
                        newWithSubdirectories.set(newDirectories.size(), other.myWithSubdirectories.get(i));
                        newDirectories.add(otherDirectory);
                    }
                }
                return new DirectoriesScope(getProject(), newDirectories.toArray(new VirtualFile[newDirectories.size()]), newWithSubdirectories);
            }
            return super.uniteWith(scope);
        }

        
        @Override
        public Project getProject() {
            return super.getProject();
        }

        
        @Override
        public String getDisplayName() {
            if (myDirectories.length == 1) {
                VirtualFile root = myDirectories[0];
                return "Directory '" + root.getName() + "'";
            }
            return "Directories " + StringUtil.join(myDirectories, new Function<VirtualFile, String>() {
                @Override
                public String fun(VirtualFile file) {
                    return "'" + file.getName() + "'";
                }
            }, ", ");
        }

    }
}
