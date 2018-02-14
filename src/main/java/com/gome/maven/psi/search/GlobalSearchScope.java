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

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.FileIndexFacade;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiBundle;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.Function;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.ContainerUtil;


import java.util.*;

public abstract class GlobalSearchScope extends SearchScope implements ProjectAwareFileFilter {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.search.GlobalSearchScope");
     private final Project myProject;

    protected GlobalSearchScope( Project project) {
        myProject = project;
    }

    protected GlobalSearchScope() {
        this(null);
    }

    public abstract boolean contains( VirtualFile file);

    
    @Override
    public Project getProject() {
        return myProject;
    }

    /**
     * @return a positive integer (+1), if file1 is located in the classpath before file2,
     *         a negative integer (-1), if file1 is located in the classpath after file2
     *         zero - otherwise or when the files are not comparable.
     */
    public abstract int compare( VirtualFile file1,  VirtualFile file2);

    // optimization methods:

    public abstract boolean isSearchInModuleContent( Module aModule);

    public boolean isSearchInModuleContent( Module aModule, boolean testSources) {
        return isSearchInModuleContent(aModule);
    }

    @Override
    public final boolean accept(VirtualFile file) {
        return contains(file);
    }

    public abstract boolean isSearchInLibraries();

    public boolean isForceSearchingInLibrarySources() {
        return false;
    }

    public boolean isSearchOutsideRootModel() {
        return false;
    }

    
    public GlobalSearchScope intersectWith( GlobalSearchScope scope) {
        if (scope == this) return this;
        if (scope instanceof IntersectionScope) {
            return scope.intersectWith(this);
        }
        return new IntersectionScope(this, scope, null);
    }

    
    @Override
    public SearchScope intersectWith( SearchScope scope2) {
        if (scope2 instanceof LocalSearchScope) {
            LocalSearchScope localScope2 = (LocalSearchScope)scope2;
            return intersectWith(localScope2);
        }
        return intersectWith((GlobalSearchScope)scope2);
    }

    
    public SearchScope intersectWith( LocalSearchScope localScope2) {
        PsiElement[] elements2 = localScope2.getScope();
        List<PsiElement> result = new ArrayList<PsiElement>(elements2.length);
        for (final PsiElement element2 : elements2) {
            if (PsiSearchScopeUtil.isInScope(this, element2)) {
                result.add(element2);
            }
        }
        return result.isEmpty() ? EMPTY_SCOPE : new LocalSearchScope(result.toArray(new PsiElement[result.size()]), null, localScope2.isIgnoreInjectedPsi());
    }

    @Override
    
    public GlobalSearchScope union( SearchScope scope) {
        if (scope instanceof GlobalSearchScope) return uniteWith((GlobalSearchScope)scope);
        return union((LocalSearchScope)scope);
    }

    
    public GlobalSearchScope union( final LocalSearchScope scope) {
        return new GlobalSearchScope(scope.getScope()[0].getProject()) {
            @Override
            public boolean contains( VirtualFile file) {
                return GlobalSearchScope.this.contains(file) || scope.isInScope(file);
            }

            @Override
            public int compare( VirtualFile file1,  VirtualFile file2) {
                return GlobalSearchScope.this.contains(file1) && GlobalSearchScope.this.contains(file2) ? GlobalSearchScope.this.compare(file1, file2) : 0;
            }

            @Override
            public boolean isSearchInModuleContent( Module aModule) {
                return GlobalSearchScope.this.isSearchInModuleContent(aModule);
            }

            @Override
            public boolean isSearchOutsideRootModel() {
                return GlobalSearchScope.this.isSearchOutsideRootModel();
            }

            @Override
            public boolean isSearchInLibraries() {
                return GlobalSearchScope.this.isSearchInLibraries();
            }

            
            @Override
            public String toString() {
                return "UnionToLocal: (" + GlobalSearchScope.this.toString() + ", " + scope + ")";
            }
        };
    }

    
    public GlobalSearchScope uniteWith( GlobalSearchScope scope) {
        if (scope == this) return scope;

        return new UnionScope(this, scope);
    }

    
    public static GlobalSearchScope union( GlobalSearchScope[] scopes) {
        if (scopes.length == 0) {
            throw new IllegalArgumentException("Empty scope array");
        }
        if (scopes.length == 1) {
            return scopes[0];
        }
        return new UnionScope(scopes);
    }

    
    public static GlobalSearchScope allScope( Project project) {
        return ProjectScope.getAllScope(project);
    }

    
    public static GlobalSearchScope projectScope( Project project) {
        return ProjectScope.getProjectScope(project);
    }

    
    public static GlobalSearchScope notScope( final GlobalSearchScope scope) {
        return new NotScope(scope);
    }
    private static class NotScope extends DelegatingGlobalSearchScope {
        private NotScope( GlobalSearchScope scope) {
            super(scope);
        }

        @Override
        public boolean contains( VirtualFile file) {
            return !myBaseScope.contains(file);
        }

        @Override
        public boolean isSearchInLibraries() {
            return true; // not (in library A) is perfectly fine to find classes in another library B.
        }

        @Override
        public boolean isSearchInModuleContent( Module aModule, boolean testSources) {
            return true; // not (some files in module A) is perfectly fine to find classes in another part of module A.
        }

        @Override
        public boolean isSearchInModuleContent( Module aModule) {
            return true; // not (some files in module A) is perfectly fine to find classes in another part of module A.
        }

        @Override
        public boolean isSearchOutsideRootModel() {
            return true;
        }

        @Override
        public String toString() {
            return "NOT: "+myBaseScope;
        }
    }

    /**
     * Returns module scope including sources and tests, excluding libraries and dependencies.
     *
     * @param module the module to get the scope.
     * @return scope including sources and tests, excluding libraries and dependencies.
     */
    
    public static GlobalSearchScope moduleScope( Module module) {
        return module.getModuleScope();
    }

    /**
     * Returns module scope including sources, tests, and libraries, excluding dependencies.
     *
     * @param module the module to get the scope.
     * @return scope including sources, tests, and libraries, excluding dependencies.
     */
    
    public static GlobalSearchScope moduleWithLibrariesScope( Module module) {
        return module.getModuleWithLibrariesScope();
    }

    /**
     * Returns module scope including sources, tests, and dependencies, excluding libraries.
     *
     * @param module the module to get the scope.
     * @return scope including sources, tests, and dependencies, excluding libraries.
     */
    
    public static GlobalSearchScope moduleWithDependenciesScope( Module module) {
        return module.getModuleWithDependenciesScope();
    }

    
    public static GlobalSearchScope moduleRuntimeScope( Module module, final boolean includeTests) {
        return module.getModuleRuntimeScope(includeTests);
    }

    
    public static GlobalSearchScope moduleWithDependenciesAndLibrariesScope( Module module) {
        return moduleWithDependenciesAndLibrariesScope(module, true);
    }

    
    public static GlobalSearchScope moduleWithDependenciesAndLibrariesScope( Module module, boolean includeTests) {
        return module.getModuleWithDependenciesAndLibrariesScope(includeTests);
    }

    
    public static GlobalSearchScope moduleWithDependentsScope( Module module) {
        return module.getModuleWithDependentsScope();
    }

    
    public static GlobalSearchScope moduleTestsWithDependentsScope( Module module) {
        return module.getModuleTestsWithDependentsScope();
    }

    
    public static GlobalSearchScope fileScope( PsiFile psiFile) {
        return new FileScope(psiFile.getProject(), psiFile.getVirtualFile());
    }

    
    public static GlobalSearchScope fileScope( Project project, final VirtualFile virtualFile) {
        return fileScope(project, virtualFile, null);
    }

    
    public static GlobalSearchScope fileScope( Project project, final VirtualFile virtualFile,  final String displayName) {
        return new FileScope(project, virtualFile) {
            
            @Override
            public String getDisplayName() {
                return displayName == null ? super.getDisplayName() : displayName;
            }
        };
    }

    
    public static GlobalSearchScope filesScope( Project project,  Collection<VirtualFile> files) {
        return filesScope(project, files, null);
    }

    
    public static GlobalSearchScope filesScope( Project project,  Collection<VirtualFile> files,  final String displayName) {
        if (files.isEmpty()) return EMPTY_SCOPE;
        return files.size() == 1? fileScope(project, files.iterator().next(), displayName) : new FilesScope(project, files) {
            
            @Override
            public String getDisplayName() {
                return displayName == null ? super.getDisplayName() : displayName;
            }
        };
    }

    private static class IntersectionScope extends GlobalSearchScope {
        private final GlobalSearchScope myScope1;
        private final GlobalSearchScope myScope2;
        private final String myDisplayName;

        private IntersectionScope( GlobalSearchScope scope1,  GlobalSearchScope scope2, String displayName) {
            super(scope1.getProject() == null ? scope2.getProject() : scope1.getProject());
            myScope1 = scope1;
            myScope2 = scope2;
            myDisplayName = displayName;
        }

        
        @Override
        public GlobalSearchScope intersectWith( GlobalSearchScope scope) {
            if (myScope1.equals(scope) || myScope2.equals(scope)) {
                return this;
            }
            return new IntersectionScope(this, scope, null);
        }

        
        @Override
        public String getDisplayName() {
            if (myDisplayName == null) {
                return PsiBundle.message("psi.search.scope.intersection", myScope1.getDisplayName(), myScope2.getDisplayName());
            }
            return myDisplayName;
        }

        @Override
        public boolean contains( VirtualFile file) {
            return myScope1.contains(file) && myScope2.contains(file);
        }

        @Override
        public int compare( VirtualFile file1,  VirtualFile file2) {
            int res1 = myScope1.compare(file1, file2);
            int res2 = myScope2.compare(file1, file2);

            if (res1 == 0) return res2;
            if (res2 == 0) return res1;

            res1 /= Math.abs(res1);
            res2 /= Math.abs(res2);
            if (res1 == res2) return res1;

            return 0;
        }

        @Override
        public boolean isSearchInModuleContent( Module aModule) {
            return myScope1.isSearchInModuleContent(aModule) && myScope2.isSearchInModuleContent(aModule);
        }

        @Override
        public boolean isSearchInModuleContent( final Module aModule, final boolean testSources) {
            return myScope1.isSearchInModuleContent(aModule, testSources) && myScope2.isSearchInModuleContent(aModule, testSources);
        }

        @Override
        public boolean isSearchInLibraries() {
            return myScope1.isSearchInLibraries() && myScope2.isSearchInLibraries();
        }

        @Override
        public boolean isSearchOutsideRootModel() {
            return myScope1.isSearchOutsideRootModel() && myScope2.isSearchOutsideRootModel();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IntersectionScope)) return false;

            IntersectionScope that = (IntersectionScope)o;

            return myScope1.equals(that.myScope1) && myScope2.equals(that.myScope2);
        }

        @Override
        public int hashCode() {
            return 31 * myScope1.hashCode() + myScope2.hashCode();
        }

        
        @Override
        public String toString() {
            return "Intersection: (" + myScope1 + ", " + myScope2 + ")";
        }
    }

    private static class UnionScope extends GlobalSearchScope {
        private final GlobalSearchScope[] myScopes;
        private final int myNestingLevel;

        private UnionScope( GlobalSearchScope scope1,  GlobalSearchScope scope2) {
            this(new GlobalSearchScope[]{scope1, scope2});
        }

        private UnionScope( GlobalSearchScope[] scopes) {
            super(ContainerUtil.getFirstItem(ContainerUtil.mapNotNull(scopes, new Function<GlobalSearchScope, Project>() {
                @Override
                public Project fun(GlobalSearchScope scope) {
                    return scope.getProject();
                }
            }), null));
            assert scopes.length > 1 : Arrays.asList(scopes);
            myScopes = scopes;
            final int[] nested = {0};
            ContainerUtil.process(scopes, new Processor<GlobalSearchScope>() {
                @Override
                public boolean process(GlobalSearchScope scope) {
                    nested[0] = Math.max(nested[0], scope instanceof UnionScope ? ((UnionScope)scope).myNestingLevel : 0);
                    return true;
                }
            });
            myNestingLevel = 1 + nested[0];
            if (myNestingLevel > 1000) {
                throw new IllegalStateException("Too many scopes combined: " + myNestingLevel + StringUtil.first(toString(), 500, true));
            }
        }

        
        @Override
        public String getDisplayName() {
            return PsiBundle.message("psi.search.scope.union", myScopes[0].getDisplayName(), myScopes[1].getDisplayName());
        }

        @Override
        public boolean contains( final VirtualFile file) {
            return ContainerUtil.find(myScopes, new Condition<GlobalSearchScope>() {
                @Override
                public boolean value(GlobalSearchScope scope) {
                    return scope.contains(file);
                }
            }) != null;
        }

        @Override
        public boolean isSearchOutsideRootModel() {
            return ContainerUtil.find(myScopes, new Condition<GlobalSearchScope>() {
                @Override
                public boolean value(GlobalSearchScope scope) {
                    return scope.isSearchOutsideRootModel();
                }
            }) != null;
        }

        @Override
        public int compare( final VirtualFile file1,  final VirtualFile file2) {
            final int[] result = {0};
            ContainerUtil.process(myScopes, new Processor<GlobalSearchScope>() {
                @Override
                public boolean process(GlobalSearchScope scope) {
                    int res1 = scope.contains(file1) && scope.contains(file2) ? scope.compare(file1, file2) : 0;
                    if (result[0] == 0) {
                        result[0] = res1;
                        return true;
                    }
                    if ((result[0] > 0) != (res1 > 0)) {
                        result[0] = 0;
                        return false;
                    }
                    return true;
                }
            });
            return result[0];
        }

        @Override
        public boolean isSearchInModuleContent( final Module module) {
            return ContainerUtil.find(myScopes, new Condition<GlobalSearchScope>() {
                @Override
                public boolean value(GlobalSearchScope scope) {
                    return scope.isSearchInModuleContent(module);
                }
            }) != null;
        }

        @Override
        public boolean isSearchInModuleContent( final Module module, final boolean testSources) {
            return ContainerUtil.find(myScopes, new Condition<GlobalSearchScope>() {
                @Override
                public boolean value(GlobalSearchScope scope) {
                    return scope.isSearchInModuleContent(module, testSources);
                }
            }) != null;
        }

        @Override
        public boolean isSearchInLibraries() {
            return ContainerUtil.find(myScopes, new Condition<GlobalSearchScope>() {
                @Override
                public boolean value(GlobalSearchScope scope) {
                    return scope.isSearchInLibraries();
                }
            }) != null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UnionScope)) return false;

            UnionScope that = (UnionScope)o;

            return new HashSet<GlobalSearchScope>(Arrays.asList(myScopes)).equals(new HashSet<GlobalSearchScope>(Arrays.asList(that.myScopes)));
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(myScopes);
        }

        
        @Override
        public String toString() {
            return "Union: (" + StringUtil.join(Arrays.asList(myScopes), ",") + ")";
        }

        
        @Override
        public GlobalSearchScope uniteWith( GlobalSearchScope scope) {
            if (scope instanceof UnionScope) {
                GlobalSearchScope[] newScopes = ArrayUtil.mergeArrays(myScopes, ((UnionScope)scope).myScopes);
                return new UnionScope(newScopes);
            }
            return super.uniteWith(scope);
        }
    }

    
    public static GlobalSearchScope getScopeRestrictedByFileTypes( GlobalSearchScope scope,  FileType... fileTypes) {
        if (scope == EMPTY_SCOPE) {
            return EMPTY_SCOPE;
        }
        LOG.assertTrue(fileTypes.length > 0);
        return new FileTypeRestrictionScope(scope, fileTypes);
    }

    private static class FileTypeRestrictionScope extends DelegatingGlobalSearchScope {
        private final FileType[] myFileTypes;

        private FileTypeRestrictionScope( GlobalSearchScope scope,  FileType[] fileTypes) {
            super(scope);
            myFileTypes = fileTypes;
        }

        @Override
        public boolean contains( VirtualFile file) {
            if (!super.contains(file)) return false;

            final FileType fileType = file.getFileType();
            for (FileType otherFileType : myFileTypes) {
                if (fileType.equals(otherFileType)) return true;
            }

            return false;
        }

        
        @Override
        public GlobalSearchScope intersectWith( GlobalSearchScope scope) {
            if (scope instanceof FileTypeRestrictionScope) {
                FileTypeRestrictionScope restrict = (FileTypeRestrictionScope)scope;
                if (restrict.myBaseScope == myBaseScope) {
                    List<FileType> intersection = new ArrayList<FileType>(Arrays.asList(restrict.myFileTypes));
                    intersection.retainAll(Arrays.asList(myFileTypes));
                    return new FileTypeRestrictionScope(myBaseScope, intersection.toArray(new FileType[intersection.size()]));
                }
            }
            return super.intersectWith(scope);
        }

        
        @Override
        public GlobalSearchScope uniteWith( GlobalSearchScope scope) {
            if (scope instanceof FileTypeRestrictionScope) {
                FileTypeRestrictionScope restrict = (FileTypeRestrictionScope)scope;
                if (restrict.myBaseScope == myBaseScope) {
                    return new FileTypeRestrictionScope(myBaseScope, ArrayUtil.mergeArrays(myFileTypes, restrict.myFileTypes));
                }
            }
            return super.uniteWith(scope);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FileTypeRestrictionScope)) return false;
            if (!super.equals(o)) return false;

            FileTypeRestrictionScope that = (FileTypeRestrictionScope)o;

            return Arrays.equals(myFileTypes, that.myFileTypes);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + Arrays.hashCode(myFileTypes);
            return result;
        }

        @Override
        public String toString() {
            return "(" + myBaseScope + " restricted by file types: "+Arrays.asList(myFileTypes)+")";
        }
    }

    private static class EmptyScope extends GlobalSearchScope {
        @Override
        public boolean contains( VirtualFile file) {
            return false;
        }

        @Override
        public int compare( VirtualFile file1,  VirtualFile file2) {
            return 0;
        }

        @Override
        public boolean isSearchInModuleContent( Module aModule) {
            return false;
        }

        @Override
        public boolean isSearchInLibraries() {
            return false;
        }

        @Override
        
        public GlobalSearchScope intersectWith( final GlobalSearchScope scope) {
            return this;
        }

        @Override
        
        public GlobalSearchScope uniteWith( final GlobalSearchScope scope) {
            return scope;
        }

        @Override
        public String toString() {
            return "EMPTY";
        }
    }

    public static final GlobalSearchScope EMPTY_SCOPE = new EmptyScope();

    private static class FileScope extends GlobalSearchScope implements Iterable<VirtualFile> {
        private final VirtualFile myVirtualFile; // files can be out of project roots
        private final Module myModule;

        private FileScope( Project project, VirtualFile virtualFile) {
            super(project);
            myVirtualFile = virtualFile;
            myModule = virtualFile == null || project.isDefault() ? null : FileIndexFacade.getInstance(project).getModuleForFile(virtualFile);
        }

        @Override
        public boolean contains( VirtualFile file) {
            return Comparing.equal(myVirtualFile, file);
        }

        @Override
        public int compare( VirtualFile file1,  VirtualFile file2) {
            return 0;
        }

        @Override
        public boolean isSearchInModuleContent( Module aModule) {
            return aModule == myModule;
        }

        @Override
        public boolean isSearchInLibraries() {
            return myModule == null;
        }

        @Override
        public String toString() {
            return "File :"+myVirtualFile;
        }

        @Override
        public Iterator<VirtualFile> iterator() {
            return Collections.singletonList(myVirtualFile).iterator();
        }
    }

    public static class FilesScope extends GlobalSearchScope implements Iterable<VirtualFile> {
        private final Collection<VirtualFile> myFiles; // files can be out of project roots

        public FilesScope(final Project project,  Collection<VirtualFile> files) {
            super(project);
            myFiles = files;
        }

        @Override
        public boolean contains( final VirtualFile file) {
            return myFiles.contains(file);
        }

        @Override
        public int compare( final VirtualFile file1,  final VirtualFile file2) {
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
        public boolean equals(Object o) {
            return this == o || o instanceof FilesScope && myFiles.equals(((FilesScope)o).myFiles);
        }

        @Override
        public int hashCode() {
            return myFiles.hashCode();
        }

        @Override
        public String toString() {
            List<VirtualFile> files = myFiles.size() <= 20 ? new ArrayList<VirtualFile>(myFiles) : new ArrayList<VirtualFile>(myFiles).subList(0,20);
            return "Files: ("+ files +")";
        }

        @Override
        public Iterator<VirtualFile> iterator() {
            return myFiles.iterator();
        }
    }
}
