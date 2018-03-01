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
package com.gome.maven.psi.impl.file;

import com.gome.maven.codeInsight.completion.scope.JavaCompletionHints;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.java.JavaLanguage;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.navigation.ItemPresentationProviders;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.ui.Queryable;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Conditions;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.JavaPsiFacadeImpl;
import com.gome.maven.psi.impl.source.tree.java.PsiCompositeModifierList;
import com.gome.maven.psi.scope.ElementClassHint;
import com.gome.maven.psi.scope.NameHint;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.search.EverythingGlobalScope;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.PsiSearchScopeUtil;
import com.gome.maven.psi.util.*;
import com.gome.maven.reference.SoftReference;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.CommonProcessors;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.*;

public class PsiPackageImpl extends PsiPackageBase implements PsiPackage, Queryable {
    private static final Logger LOG = Logger.getInstance(PsiPackageImpl.class);

    private volatile CachedValue<PsiModifierList> myAnnotationList;
    private volatile CachedValue<Collection<PsiDirectory>> myDirectories;
    private volatile CachedValue<Collection<PsiDirectory>> myDirectoriesWithLibSources;
    private volatile SoftReference<Map<String, PsiClass[]>> myClassCache;

    public PsiPackageImpl(PsiManager manager, String qualifiedName) {
        super(manager, qualifiedName);
    }

    @Override
    protected Collection<PsiDirectory> getAllDirectories(boolean includeLibrarySources) {
        if (includeLibrarySources) {
            if (myDirectoriesWithLibSources == null) {
                myDirectoriesWithLibSources = createCachedDirectories(true);
            }
            return myDirectoriesWithLibSources.getValue();
        }
        else {
            if (myDirectories == null) {
                myDirectories = createCachedDirectories(false);
            }
            return myDirectories.getValue();
        }
    }

    
    private CachedValue<Collection<PsiDirectory>> createCachedDirectories(final boolean includeLibrarySources) {
        return CachedValuesManager.getManager(myManager.getProject()).createCachedValue(new CachedValueProvider<Collection<PsiDirectory>>() {
            @Override
            public Result<Collection<PsiDirectory>> compute() {
                final CommonProcessors.CollectProcessor<PsiDirectory> processor = new CommonProcessors.CollectProcessor<PsiDirectory>();
                getFacade().processPackageDirectories(PsiPackageImpl.this, allScope(), processor, includeLibrarySources);
                return Result.create(processor.getResults(), PsiPackageImplementationHelper.getInstance().getDirectoryCachedValueDependencies(
                        PsiPackageImpl.this));
            }
        }, false);
    }

    @Override
    protected PsiElement findPackage(String qName) {
        return getFacade().findPackage(qName);
    }

    @Override
    public void handleQualifiedNameChange( final String newQualifiedName) {
        PsiPackageImplementationHelper.getInstance().handleQualifiedNameChange(this, newQualifiedName);
    }

    @Override
    public VirtualFile[] occursInPackagePrefixes() {
        return PsiPackageImplementationHelper.getInstance().occursInPackagePrefixes(this);
    }

    @Override
    public PsiPackageImpl getParentPackage() {
        return (PsiPackageImpl)super.getParentPackage();
    }


    @Override
    protected PsiPackageImpl createInstance(PsiManager manager, String qName) {
        return new PsiPackageImpl(myManager, qName);
    }

    @Override
    
    public Language getLanguage() {
        return JavaLanguage.INSTANCE;
    }

    @Override
    public boolean isValid() {
        return PsiPackageImplementationHelper.getInstance().packagePrefixExists(this) || !getAllDirectories(true).isEmpty();
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitPackage(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override
    public String toString() {
        return "PsiPackage:" + getQualifiedName();
    }

    @Override
    
    public PsiClass[] getClasses() {
        return getClasses(allScope());
    }

    
    protected GlobalSearchScope allScope() {
        return PsiPackageImplementationHelper.getInstance().adjustAllScope(this, GlobalSearchScope.allScope(getProject()));
    }

    @Override
    
    public PsiClass[] getClasses( GlobalSearchScope scope) {
        return getFacade().getClasses(this, scope);
    }

    @Override
    public PsiFile[] getFiles( GlobalSearchScope scope) {
        return getFacade().getPackageFiles(this, scope);
    }

    @Override
    
    public PsiModifierList getAnnotationList() {
        if (myAnnotationList == null) {
            myAnnotationList = CachedValuesManager.getManager(myManager.getProject()).createCachedValue(new PackageAnnotationValueProvider());
        }
        return myAnnotationList.getValue();
    }

    @Override
    
    public PsiPackage[] getSubPackages() {
        return getSubPackages(allScope());
    }

    @Override
    
    public PsiPackage[] getSubPackages( GlobalSearchScope scope) {
        return getFacade().getSubPackages(this, scope);
    }

    private JavaPsiFacadeImpl getFacade() {
        return (JavaPsiFacadeImpl)JavaPsiFacade.getInstance(myManager.getProject());
    }

    
    private PsiClass[] getCachedClassesByName( String name) {
        if (DumbService.getInstance(getProject()).isDumb()) {
            return getCachedClassInDumbMode(name);
        }

        Map<String, PsiClass[]> map = SoftReference.dereference(myClassCache);
        if (map == null) {
            myClassCache = new SoftReference<Map<String, PsiClass[]>>(map = ContainerUtil.createConcurrentSoftValueMap());
        }
        PsiClass[] classes = map.get(name);
        if (classes != null) {
            return classes;
        }

        final String qName = getQualifiedName();
        final String classQName = !qName.isEmpty() ? qName + "." + name : name;
        map.put(name, classes = getFacade().findClasses(classQName, new EverythingGlobalScope(getProject())));
        return classes;
    }

    private PsiClass[] getCachedClassInDumbMode(String name) {
        Map<String, PsiClass[]> map = SoftReference.dereference(myClassCache);
        if (map == null) {
            map = new HashMap<String, PsiClass[]>();
            for (PsiClass psiClass : getClasses(new EverythingGlobalScope(getProject()))) {
                String psiClassName = psiClass.getName();
                if (psiClassName != null) {
                    PsiClass[] existing = map.get(psiClassName);
                    map.put(psiClassName, existing == null ? new PsiClass[]{psiClass} : ArrayUtil.append(existing, psiClass));
                }
            }
            myClassCache = new SoftReference<Map<String, PsiClass[]>>(map);
        }
        PsiClass[] classes = map.get(name);
        return classes == null ? PsiClass.EMPTY_ARRAY : classes;
    }

    @Override
    public boolean containsClassNamed( String name) {
        return getCachedClassesByName(name).length > 0;
    }

    
    @Override
    public PsiClass[] findClassByShortName( String name,  final GlobalSearchScope scope) {
        PsiClass[] allClasses = getCachedClassesByName(name);
        if (allClasses.length == 0) return allClasses;
        if (allClasses.length == 1) {
            return PsiSearchScopeUtil.isInScope(scope, allClasses[0]) ? allClasses : PsiClass.EMPTY_ARRAY;
        }
        PsiClass[] array = ContainerUtil.findAllAsArray(allClasses, new Condition<PsiClass>() {
            @Override
            public boolean value(PsiClass aClass) {
                return PsiSearchScopeUtil.isInScope(scope, aClass);
            }
        });
        Arrays.sort(array, new Comparator<PsiClass>() {
            @Override
            public int compare(PsiClass o1, PsiClass o2) {
                VirtualFile file1 = o1.getContainingFile().getVirtualFile();
                VirtualFile file2 = o2.getContainingFile().getVirtualFile();
                if (file1 == null) return file2 == null ? 0 : -1;
                if (file2 == null) return 1;
                return scope.compare(file2, file1);
            }
        });
        return array;
    }

    
    private PsiPackage findSubPackageByName( String name) {
        final String qName = getQualifiedName();
        final String subpackageQName = qName.isEmpty() ? name : qName + "." + name;
        return getFacade().findPackage(subpackageQName);
    }

    @Override
    public boolean processDeclarations( PsiScopeProcessor processor,
                                        ResolveState state,
                                       PsiElement lastParent,
                                        PsiElement place) {
        GlobalSearchScope scope = place.getResolveScope();

        processor.handleEvent(PsiScopeProcessor.Event.SET_DECLARATION_HOLDER, this);
        ElementClassHint classHint = processor.getHint(ElementClassHint.KEY);

        final Condition<String> nameCondition = processor.getHint(JavaCompletionHints.NAME_FILTER);

        if (classHint == null || classHint.shouldProcess(ElementClassHint.DeclarationKind.CLASS)) {
            NameHint nameHint = processor.getHint(NameHint.KEY);
            if (nameHint != null) {
                final String shortName = nameHint.getName(state);
                final PsiClass[] classes = findClassByShortName(shortName, scope);
                if (!processClasses(processor, state, classes, Conditions.<String>alwaysTrue())) return false;
            }
            else {
                PsiClass[] classes = getClasses(scope);
                if (!processClasses(processor, state, classes, nameCondition != null ? nameCondition : Conditions.<String>alwaysTrue())) return false;
            }
        }
        if (classHint == null || classHint.shouldProcess(ElementClassHint.DeclarationKind.PACKAGE)) {
            NameHint nameHint = processor.getHint(NameHint.KEY);
            if (nameHint != null) {
                PsiPackage aPackage = findSubPackageByName(nameHint.getName(state));
                if (aPackage != null) {
                    if (!processor.execute(aPackage, state)) return false;
                }
            }
            else {
                PsiPackage[] packs = getSubPackages(scope);
                for (PsiPackage pack : packs) {
                    final String packageName = pack.getName();
                    if (packageName == null) continue;
                    if (!PsiNameHelper.getInstance(myManager.getProject()).isIdentifier(packageName, PsiUtil.getLanguageLevel(this))) {
                        continue;
                    }
                    if (!processor.execute(pack, state)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean processClasses( PsiScopeProcessor processor,
                                           ResolveState state,
                                           PsiClass[] classes,
                                           Condition<String> nameCondition) {
        for (PsiClass aClass : classes) {
            String name = aClass.getName();
            if (name != null && nameCondition.value(name)) {
                try {
                    if (!processor.execute(aClass, state)) return false;
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (Exception e) {
                    LOG.error(e);
                }
            }
        }
        return true;
    }

    @Override
    public boolean canNavigate() {
        return isValid();
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    public void navigate(final boolean requestFocus) {
        PsiPackageImplementationHelper.getInstance().navigate(this, requestFocus);
    }

    private class PackageAnnotationValueProvider implements CachedValueProvider<PsiModifierList> {
        private final Object[] OOCB_DEPENDENCY = { PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT };

        @Override
        public Result<PsiModifierList> compute() {
            List<PsiModifierList> list = new ArrayList<PsiModifierList>();
            for(PsiDirectory directory: getDirectories()) {
                PsiFile file = directory.findFile(PACKAGE_INFO_FILE);
                if (file != null) {
                    PsiPackageStatement stmt = PsiTreeUtil.getChildOfType(file, PsiPackageStatement.class);
                    if (stmt != null) {
                        final PsiModifierList modifierList = stmt.getAnnotationList();
                        if (modifierList != null) {
                            list.add(modifierList);
                        }
                    }
                }
            }

            final JavaPsiFacade facade = getFacade();
            final GlobalSearchScope scope = allScope();
            for (PsiClass aClass : facade.findClasses(getQualifiedName() + ".package-info", scope)) {
                ContainerUtil.addIfNotNull(aClass.getModifierList(), list);
            }

            return new Result<PsiModifierList>(list.isEmpty() ? null : new PsiCompositeModifierList(getManager(), list), OOCB_DEPENDENCY);
        }
    }

    @Override
    
    public PsiModifierList getModifierList() {
        return getAnnotationList();
    }

    @Override
    public boolean hasModifierProperty(  final String name) {
        return false;
    }

    @Override
    public PsiQualifiedNamedElement getContainer() {
        return getParentPackage();
    }
}
