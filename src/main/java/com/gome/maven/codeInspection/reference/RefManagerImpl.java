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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Oct 22, 2001
 * Time: 8:21:36 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.codeInspection.reference;

import com.gome.maven.ToolExtensionPoints;
import com.gome.maven.analysis.AnalysisScope;
import com.gome.maven.codeInspection.GlobalInspectionContext;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.codeInspection.lang.InspectionExtensionsFactory;
import com.gome.maven.codeInspection.lang.RefManagerExtension;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.PathMacroManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.extensions.ExtensionPoint;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectUtilCore;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.NullableFactory;
import com.gome.maven.openapi.util.Segment;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.light.LightElement;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import org.jdom.Element;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RefManagerImpl extends RefManager {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInspection.reference.RefManager");

    private long myLastUsedMask = 256 * 256 * 256 * 8;

    
    private final Project myProject;
    private AnalysisScope myScope;
    private RefProject myRefProject;
    private Map<PsiAnchor, RefElement> myRefTable = new THashMap<PsiAnchor, RefElement>();

    private Map<Module, RefModule> myModules;
    private final ProjectIterator myProjectIterator;
    private volatile boolean myDeclarationsFound;
    private final PsiManager myPsiManager;

    private volatile boolean myIsInProcess = false;

    private final List<RefGraphAnnotator> myGraphAnnotators = new ArrayList<RefGraphAnnotator>();
    private GlobalInspectionContext myContext;

    private final Map<Key, RefManagerExtension> myExtensions = new HashMap<Key, RefManagerExtension>();
    private final Map<Language, RefManagerExtension> myLanguageExtensions = new HashMap<Language, RefManagerExtension>();

    private final ReentrantReadWriteLock myLock = new ReentrantReadWriteLock();

    public RefManagerImpl( Project project,  AnalysisScope scope,  GlobalInspectionContext context) {
        myDeclarationsFound = false;
        myProject = project;
        myScope = scope;
        myContext = context;
        myPsiManager = PsiManager.getInstance(project);
        myRefProject = new RefProjectImpl(this);
        myProjectIterator = new ProjectIterator();
        for (InspectionExtensionsFactory factory : Extensions.getExtensions(InspectionExtensionsFactory.EP_NAME)) {
            final RefManagerExtension extension = factory.createRefManagerExtension(this);
            if (extension != null) {
                myExtensions.put(extension.getID(), extension);
                myLanguageExtensions.put(extension.getLanguage(), extension);
            }
        }
        if (scope != null) {
            for (Module module : ModuleManager.getInstance(getProject()).getModules()) {
                getRefModule(module);
            }
        }
    }

    
    public GlobalInspectionContext getContext() {
        return myContext;
    }

    @Override
    public void iterate( RefVisitor visitor) {
        myLock.readLock().lock();
        try {
            for (RefElement refElement : getSortedElements()) {
                refElement.accept(visitor);
            }
            if (myModules != null) {
                for (RefModule refModule : myModules.values()) {
                    refModule.accept(visitor);
                }
            }
            for (RefManagerExtension extension : myExtensions.values()) {
                extension.iterate(visitor);
            }
        }
        finally {
            myLock.readLock().unlock();
        }
    }

    public void cleanup() {
        myScope = null;
        myRefProject = null;
        myRefTable = null;
        myModules = null;
        myContext = null;

        myGraphAnnotators.clear();
        for (RefManagerExtension extension : myExtensions.values()) {
            extension.cleanup();
        }
    }

    
    @Override
    public AnalysisScope getScope() {
        return myScope;
    }


    public void fireNodeInitialized(RefElement refElement) {
        for (RefGraphAnnotator annotator : myGraphAnnotators) {
            annotator.onInitialize(refElement);
        }
    }

    public void fireNodeMarkedReferenced(RefElement refWhat,
                                         RefElement refFrom,
                                         boolean referencedFromClassInitializer,
                                         final boolean forReading,
                                         final boolean forWriting) {
        for (RefGraphAnnotator annotator : myGraphAnnotators) {
            annotator.onMarkReferenced(refWhat, refFrom, referencedFromClassInitializer, forReading, forWriting);
        }
    }

    public void fireNodeMarkedReferenced(PsiElement what,
                                         PsiElement from,
                                         boolean referencedFromClassInitializer) {
        for (RefGraphAnnotator annotator : myGraphAnnotators) {
            annotator.onMarkReferenced(what, from, referencedFromClassInitializer);
        }
    }

    public void fireBuildReferences(RefElement refElement) {
        for (RefGraphAnnotator annotator : myGraphAnnotators) {
            annotator.onReferencesBuild(refElement);
        }
    }

    public void registerGraphAnnotator(RefGraphAnnotator annotator) {
        myGraphAnnotators.add(annotator);
    }

    @Override
    public long getLastUsedMask() {
        myLastUsedMask *= 2;
        return myLastUsedMask;
    }

    @Override
    public <T> T getExtension( final Key<T> key) {
        return (T)myExtensions.get(key);
    }

    @Override
    
    public String getType(final RefEntity ref) {
        for (RefManagerExtension extension : myExtensions.values()) {
            final String type = extension.getType(ref);
            if (type != null) return type;
        }
        if (ref instanceof RefFile) {
            return SmartRefElementPointer.FILE;
        }
        if (ref instanceof RefModule) {
            return SmartRefElementPointer.MODULE;
        }
        if (ref instanceof RefProject) {
            return SmartRefElementPointer.PROJECT;
        }
        if (ref instanceof RefDirectory) {
            return SmartRefElementPointer.DIR;
        }
        return null;
    }

    
    @Override
    public RefEntity getRefinedElement( RefEntity ref) {
        for (RefManagerExtension extension : myExtensions.values()) {
            ref = extension.getRefinedElement(ref);
        }
        return ref;
    }

    @Override
    public Element export( RefEntity refEntity,  final Element element, final int actualLine) {
        refEntity = getRefinedElement(refEntity);

        Element problem = new Element("problem");

        if (refEntity instanceof RefElement) {
            final RefElement refElement = (RefElement)refEntity;
            final SmartPsiElementPointer pointer = refElement.getPointer();
            PsiFile psiFile = pointer.getContainingFile();
            if (psiFile == null) return null;

            Element fileElement = new Element("file");
            Element lineElement = new Element("line");
            final VirtualFile virtualFile = psiFile.getVirtualFile();
            LOG.assertTrue(virtualFile != null);
            fileElement.addContent(virtualFile.getUrl());

            if (actualLine == -1) {
                final Document document = PsiDocumentManager.getInstance(pointer.getProject()).getDocument(psiFile);
                LOG.assertTrue(document != null);
                final Segment range = pointer.getRange();
                lineElement.addContent(String.valueOf(range != null ? document.getLineNumber(range.getStartOffset()) + 1 : -1));
            }
            else {
                lineElement.addContent(String.valueOf(actualLine));
            }

            problem.addContent(fileElement);
            problem.addContent(lineElement);

            appendModule(problem, refElement.getModule());
        }
        else if (refEntity instanceof RefModule) {
            final RefModule refModule = (RefModule)refEntity;
            final VirtualFile moduleFile = refModule.getModule().getModuleFile();
            final Element fileElement = new Element("file");
            fileElement.addContent(moduleFile != null ? moduleFile.getUrl() : refEntity.getName());
            problem.addContent(fileElement);
            appendModule(problem, refModule);
        }

        for (RefManagerExtension extension : myExtensions.values()) {
            extension.export(refEntity, problem);
        }

        new SmartRefElementPointerImpl(refEntity, true).writeExternal(problem);
        element.addContent(problem);
        return problem;
    }

    @Override
    
    public String getGroupName(final RefElement entity) {
        for (RefManagerExtension extension : myExtensions.values()) {
            final String groupName = extension.getGroupName(entity);
            if (groupName != null) return groupName;
        }
        return null;
    }

    private static void appendModule(final Element problem, final RefModule refModule) {
        if (refModule != null) {
            Element moduleElement = new Element("module");
            moduleElement.addContent(refModule.getName());
            problem.addContent(moduleElement);
        }
    }

    public void findAllDeclarations() {
        if (!myDeclarationsFound) {
            long before = System.currentTimeMillis();
            final AnalysisScope scope = getScope();
            if (scope != null) {
                scope.accept(myProjectIterator);
            }
            myDeclarationsFound = true;

            LOG.info("Total duration of processing project usages:" + (System.currentTimeMillis() - before));
        }
    }

    public boolean isDeclarationsFound() {
        return myDeclarationsFound;
    }

    public void inspectionReadActionStarted() {
        myIsInProcess = true;
    }

    public void inspectionReadActionFinished() {
        myIsInProcess = false;
    }


    public boolean isInProcess() {
        return myIsInProcess;
    }

    
    @Override
    public Project getProject() {
        return myProject;
    }

    
    @Override
    public RefProject getRefProject() {
        return myRefProject;
    }

    
    public List<RefElement> getSortedElements() {
        LOG.assertTrue(myRefTable != null);
        List<RefElement> answer = new ArrayList<RefElement>(myRefTable.values());
        ContainerUtil.quickSort(answer, new Comparator<RefElement>() {
            @Override
            public int compare(RefElement o1, RefElement o2) {
                VirtualFile v1 = ((RefElementImpl)o1).getVirtualFile();
                VirtualFile v2 = ((RefElementImpl)o2).getVirtualFile();

                return (v1 != null ? v1.hashCode() : 0) - (v2 != null ? v2.hashCode() : 0);
            }
        });

        return answer;
    }

    
    @Override
    public PsiManager getPsiManager() {
        return myPsiManager;
    }

    public void removeReference( RefElement refElem) {
        myLock.writeLock().lock();
        try {
            final Map<PsiAnchor, RefElement> refTable = myRefTable;
            final PsiElement element = refElem.getElement();
            final RefManagerExtension extension = element != null ? getExtension(element.getLanguage()) : null;
            if (extension != null) {
                extension.removeReference(refElem);
            }

            if (element != null && refTable.remove(ApplicationManager.getApplication().runReadAction(
                    new Computable<PsiAnchor>() {
                        @Override
                        public PsiAnchor compute() {
                            return PsiAnchor.create(element);
                        }
                    }
            )) != null) return;

            //PsiElement may have been invalidated and new one returned by getElement() is different so we need to do this stuff.
            for (PsiAnchor psiElement : refTable.keySet()) {
                if (refTable.get(psiElement) == refElem) {
                    refTable.remove(psiElement);
                    return;
                }
            }
        }
        finally {
            myLock.writeLock().unlock();
        }
    }

    public void initializeAnnotators() {
        ExtensionPoint<RefGraphAnnotator> point = Extensions.getRootArea().getExtensionPoint(ToolExtensionPoints.INSPECTIONS_GRAPH_ANNOTATOR);
        final RefGraphAnnotator[] graphAnnotators = point.getExtensions();
        for (RefGraphAnnotator annotator : graphAnnotators) {
            registerGraphAnnotator(annotator);
        }
        for (RefGraphAnnotator graphAnnotator : myGraphAnnotators) {
            if (graphAnnotator instanceof RefGraphAnnotatorEx) {
                ((RefGraphAnnotatorEx)graphAnnotator).initialize(this);
            }
        }
    }

    private class ProjectIterator extends PsiElementVisitor {
        @Override
        public void visitElement(PsiElement element) {
            final RefManagerExtension extension = getExtension(element.getLanguage());
            if (extension != null) {
                extension.visitElement(element);
            }
            for (PsiElement aChildren : element.getChildren()) {
                aChildren.accept(this);
            }
        }

        @Override
        public void visitFile(PsiFile file) {
            final VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                String relative = ProjectUtilCore.displayUrlRelativeToProject(virtualFile, virtualFile.getPresentableUrl(), myProject, true, false);
                myContext.incrementJobDoneAmount(myContext.getStdJobDescriptors().BUILD_GRAPH, relative);
            }
            final FileViewProvider viewProvider = file.getViewProvider();
            final Set<Language> relevantLanguages = viewProvider.getLanguages();
            for (Language language : relevantLanguages) {
                visitElement(viewProvider.getPsi(language));
            }
            myPsiManager.dropResolveCaches();
            InjectedLanguageManager.getInstance(myProject).dropFileCaches(file);
        }
    }

    @Override
    
    public RefElement getReference(final PsiElement elem) {
        return getReference(elem, false);
    }

    
    public RefElement getReference(final PsiElement elem, final boolean ignoreScope) {
        if (ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                return elem == null || !elem.isValid() ||
                        elem instanceof LightElement || !(elem instanceof PsiDirectory) && !belongsToScope(elem, ignoreScope);
            }
        })) {
            return null;
        }

        return getFromRefTableOrCache(
                elem,
                new NullableFactory<RefElementImpl>() {
                    @Override
                    public RefElementImpl create() {
                        return ApplicationManager.getApplication().runReadAction(new Computable<RefElementImpl>() {
                            @Override
                            
                            public RefElementImpl compute() {
                                final RefManagerExtension extension = getExtension(elem.getLanguage());
                                if (extension != null) {
                                    final RefElement refElement = extension.createRefElement(elem);
                                    if (refElement != null) return (RefElementImpl)refElement;
                                }
                                if (elem instanceof PsiFile) {
                                    return new RefFileImpl((PsiFile)elem, RefManagerImpl.this);
                                }
                                if (elem instanceof PsiDirectory) {
                                    return new RefDirectoryImpl((PsiDirectory)elem, RefManagerImpl.this);
                                }
                                return null;
                            }
                        });
                    }
                },
                new Consumer<RefElementImpl>() {
                    @Override
                    public void consume(RefElementImpl element) {
                        element.initialize();
                        for (RefManagerExtension each : myExtensions.values()) {
                            each.onEntityInitialized(element, elem);
                        }
                        fireNodeInitialized(element);
                    }
                });
    }

    private RefManagerExtension getExtension(final Language language) {
        return myLanguageExtensions.get(language);
    }

    
    @Override
    public RefEntity getReference(final String type, final String fqName) {
        for (RefManagerExtension extension : myExtensions.values()) {
            final RefEntity refEntity = extension.getReference(type, fqName);
            if (refEntity != null) return refEntity;
        }
        if (SmartRefElementPointer.FILE.equals(type)) {
            return RefFileImpl.fileFromExternalName(this, fqName);
        }
        if (SmartRefElementPointer.MODULE.equals(type)) {
            return RefModuleImpl.moduleFromName(this, fqName);
        }
        if (SmartRefElementPointer.PROJECT.equals(type)) {
            return getRefProject();
        }
        if (SmartRefElementPointer.DIR.equals(type)) {
            String url = VfsUtilCore.pathToUrl(PathMacroManager.getInstance(getProject()).expandPath(fqName));
            VirtualFile vFile = VirtualFileManager.getInstance().findFileByUrl(url);
            if (vFile != null) {
                final PsiDirectory dir = PsiManager.getInstance(getProject()).findDirectory(vFile);
                return getReference(dir);
            }
        }
        return null;
    }

    
    protected <T extends RefElement> T getFromRefTableOrCache(final PsiElement element,
                                                               NullableFactory<T> factory) {
        return getFromRefTableOrCache(element, factory, null);
    }

    
    protected <T extends RefElement> T getFromRefTableOrCache(final PsiElement element,
                                                               NullableFactory<T> factory,
                                                               Consumer<T> whenCached) {
        T result;

        myLock.readLock().lock();
        try {
            //noinspection unchecked
            result = (T)myRefTable.get(ApplicationManager.getApplication().runReadAction(
                    new Computable<PsiAnchor>() {
                        @Override
                        public PsiAnchor compute() {
                            return PsiAnchor.create(element);
                        }
                    }
            ));
        }
        finally {
            myLock.readLock().unlock();
        }

        if (result != null) return result;

        if (!isValidPointForReference()) {
            //LOG.assertTrue(true, "References may become invalid after process is finished");
            return null;
        }

        myLock.writeLock().lock();
        try {
            //noinspection unchecked
            result = (T)myRefTable.get(ApplicationManager.getApplication().runReadAction(
                    new Computable<PsiAnchor>() {
                        @Override
                        public PsiAnchor compute() {
                            return PsiAnchor.create(element);
                        }
                    }
            ));
            if (result != null) return result;

            result = factory.create();
            if (result == null) return null;

            myRefTable.put(ApplicationManager.getApplication().runReadAction(
                    new Computable<PsiAnchor>() {
                        @Override
                        public PsiAnchor compute() {
                            return PsiAnchor.create(element);
                        }
                    }
            ), result);
        }
        finally {
            myLock.writeLock().unlock();
        }

        if (whenCached != null) whenCached.consume(result);

        return result;
    }

    @Override
    public RefModule getRefModule(Module module) {
        if (module == null) {
            return null;
        }
        myLock.readLock().lock();
        try {
            if (myModules != null) {
                RefModule refModule = myModules.get(module);
                if (refModule != null) {
                    return refModule;
                }
            }
        }
        finally {
            myLock.readLock().unlock();
        }

        myLock.writeLock().lock();
        try {
            if (myModules == null) {
                myModules = new THashMap<Module, RefModule>();
            }
            final RefModule refModule = new RefModuleImpl(module, this);
            myModules.put(module, refModule);
            return refModule;
        }
        finally {
            myLock.writeLock().unlock();
        }
    }

    @Override
    public boolean belongsToScope(final PsiElement psiElement) {
        return belongsToScope(psiElement, false);
    }

    private boolean belongsToScope(final PsiElement psiElement, final boolean ignoreScope) {
        if (psiElement == null || !psiElement.isValid()) return false;
        if (psiElement instanceof PsiCompiledElement) return false;
        final PsiFile containingFile = ApplicationManager.getApplication().runReadAction(new Computable<PsiFile>() {
            @Override
            public PsiFile compute() {
                return psiElement.getContainingFile();
            }
        });
        if (containingFile == null) {
            return false;
        }
        for (RefManagerExtension extension : myExtensions.values()) {
            if (!extension.belongsToScope(psiElement)) return false;
        }
        final Boolean inProject = ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                return psiElement.getManager().isInProject(psiElement);
            }
        });
        return inProject.booleanValue() && (ignoreScope || getScope() == null || getScope().contains(psiElement));
    }

    @Override
    public String getQualifiedName(RefEntity refEntity) {
        if (refEntity == null || refEntity instanceof RefElementImpl && !refEntity.isValid()) {
            return InspectionsBundle.message("inspection.reference.invalid");
        }

        return refEntity.getQualifiedName();
    }

    @Override
    public void removeRefElement( RefElement refElement,  List<RefElement> deletedRefs) {
        List<RefEntity> children = refElement.getChildren();
        if (children != null) {
            RefElement[] refElements = children.toArray(new RefElement[children.size()]);
            for (RefElement refChild : refElements) {
                removeRefElement(refChild, deletedRefs);
            }
        }

        ((RefManagerImpl)refElement.getRefManager()).removeReference(refElement);
        ((RefElementImpl)refElement).referenceRemoved();
        if (!deletedRefs.contains(refElement)) deletedRefs.add(refElement);
    }

    protected boolean isValidPointForReference() {
        return myIsInProcess || ApplicationManager.getApplication().isUnitTestMode();
    }
}
