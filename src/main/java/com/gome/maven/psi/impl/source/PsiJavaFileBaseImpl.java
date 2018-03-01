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
package com.gome.maven.psi.impl.source;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.java.JavaLanguage;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.LanguageLevelProjectExtension;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.NotNullLazyKey;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.JavaCodeStyleManager;
import com.gome.maven.psi.impl.JavaPsiImplementationHelper;
import com.gome.maven.psi.impl.PsiFileEx;
import com.gome.maven.psi.impl.PsiImplUtil;
import com.gome.maven.psi.impl.java.stubs.JavaStubElementTypes;
import com.gome.maven.psi.impl.java.stubs.PsiJavaFileStub;
import com.gome.maven.psi.impl.source.resolve.ClassResolverProcessor;
import com.gome.maven.psi.impl.source.resolve.SymbolCollectingProcessor;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.scope.ElementClassHint;
import com.gome.maven.psi.scope.JavaScopeProcessorEvent;
import com.gome.maven.psi.scope.NameHint;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.util.*;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.NotNullFunction;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.HashSet;
import com.gome.maven.util.containers.MostlySingularMultiMap;
import com.gome.maven.util.indexing.IndexingDataKeys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class PsiJavaFileBaseImpl extends PsiFileImpl implements PsiJavaFile {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.PsiJavaFileBaseImpl");
     private static final String[] IMPLICIT_IMPORTS = { CommonClassNames.DEFAULT_PACKAGE };
    private final CachedValue<MostlySingularMultiMap<String, SymbolCollectingProcessor.ResultWithContext>> myResolveCache;
    private volatile String myPackageName;

    protected PsiJavaFileBaseImpl(IElementType elementType, IElementType contentElementType, FileViewProvider viewProvider) {
        super(elementType, contentElementType, viewProvider);
        myResolveCache = CachedValuesManager.getManager(myManager.getProject()).createCachedValue(new MyCacheBuilder(this), false);
    }

    @Override
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    protected PsiJavaFileBaseImpl clone() {
        PsiFileImpl clone = super.clone();
        if (!(clone instanceof PsiJavaFileBaseImpl)) {
            throw new AssertionError("Java file cloned as text: " + getTextLength() + "; " + getViewProvider());
        }
        clone.clearCaches();
        return (PsiJavaFileBaseImpl)clone;
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        myPackageName = null;
    }

    @Override
    
    public PsiClass[] getClasses() {
        final StubElement<?> stub = getStub();
        if (stub != null) {
            return stub.getChildrenByType(JavaStubElementTypes.CLASS, PsiClass.ARRAY_FACTORY);
        }

        return calcTreeElement().getChildrenAsPsiElements(Constants.CLASS_BIT_SET, PsiClass.ARRAY_FACTORY);
    }

    @Override
    public PsiPackageStatement getPackageStatement() {
        ASTNode node = calcTreeElement().findChildByType(JavaElementType.PACKAGE_STATEMENT);
        return node != null ? (PsiPackageStatement)node.getPsi() : null;
    }

    @Override
    
    public String getPackageName() {
        PsiJavaFileStub stub = (PsiJavaFileStub)getStub();
        if (stub != null) {
            return stub.getPackageName();
        }

        String name = myPackageName;
        if (name == null) {
            PsiPackageStatement statement = getPackageStatement();
            myPackageName = name = statement == null ? "" : statement.getPackageName();
        }
        return name;
    }

    @Override
    public void setPackageName(final String packageName) throws IncorrectOperationException {
        final PsiPackageStatement packageStatement = getPackageStatement();
        final PsiElementFactory factory = JavaPsiFacade.getInstance(getProject()).getElementFactory();
        if (packageStatement != null) {
            if (!packageName.isEmpty()) {
                final PsiJavaCodeReferenceElement reference = packageStatement.getPackageReference();
                reference.replace(factory.createReferenceFromText(packageName, packageStatement));
            }
            else {
                packageStatement.delete();
            }
        }
        else {
            if (!packageName.isEmpty()) {
                addBefore(factory.createPackageStatement(packageName), getFirstChild());
            }
        }
    }

    @Override
    
    public PsiImportList getImportList() {
        StubElement<?> stub = getStub();
        if (stub != null) {
            PsiImportList[] nodes = stub.getChildrenByType(JavaStubElementTypes.IMPORT_LIST, PsiImportList.ARRAY_FACTORY);
            if (nodes.length != 1) {
                reportStubAstMismatch(stub + "; " + stub.getChildrenStubs(), getStubTree(), PsiDocumentManager.getInstance(getProject()).getCachedDocument(this));
            }
            return nodes[0];
        }

        ASTNode node = calcTreeElement().findChildByType(JavaElementType.IMPORT_LIST);
        assert node != null : getFileType() + ", " + getName();
        return SourceTreeToPsiMap.treeToPsiNotNull(node);
    }

    @Override
    
    public PsiElement[] getOnDemandImports(boolean includeImplicit, boolean checkIncludes) {
        List<PsiElement> array = new ArrayList<PsiElement>();

        PsiImportList importList = getImportList();
        PsiImportStatement[] statements = importList.getImportStatements();
        for (PsiImportStatement statement : statements) {
            if (statement.isOnDemand()) {
                PsiElement resolved = statement.resolve();
                if (resolved != null) {
                    array.add(resolved);
                }
            }
        }

        if (includeImplicit){
            PsiJavaCodeReferenceElement[] implicitRefs = getImplicitlyImportedPackageReferences();
            for (PsiJavaCodeReferenceElement implicitRef : implicitRefs) {
                final PsiElement resolved = implicitRef.resolve();
                if (resolved != null) {
                    array.add(resolved);
                }
            }
        }

        return PsiUtilCore.toPsiElementArray(array);
    }

    @Override
    
    public PsiClass[] getSingleClassImports(boolean checkIncludes) {
        List<PsiClass> array = new ArrayList<PsiClass>();
        PsiImportList importList = getImportList();
        PsiImportStatement[] statements = importList.getImportStatements();
        for (PsiImportStatement statement : statements) {
            if (!statement.isOnDemand()) {
                PsiElement ref = statement.resolve();
                if (ref instanceof PsiClass) {
                    array.add((PsiClass)ref);
                }
            }
        }
        return array.toArray(new PsiClass[array.size()]);
    }

    @Override
    public PsiJavaCodeReferenceElement findImportReferenceTo(PsiClass aClass) {
        PsiImportList importList = getImportList();
        PsiImportStatement[] statements = importList.getImportStatements();
        for (PsiImportStatement statement : statements) {
            if (!statement.isOnDemand()) {
                PsiElement ref = statement.resolve();
                if (ref != null && getManager().areElementsEquivalent(ref, aClass)) {
                    return statement.getImportReference();
                }
            }
        }
        return null;
    }

    @Override
    
    public String[] getImplicitlyImportedPackages() {
        return IMPLICIT_IMPORTS;
    }

    @Override
    
    public PsiJavaCodeReferenceElement[] getImplicitlyImportedPackageReferences() {
        return PsiImplUtil.namesToPackageReferences(myManager, IMPLICIT_IMPORTS);
    }

    private static class StaticImportFilteringProcessor implements PsiScopeProcessor {
        private final PsiScopeProcessor myDelegate;
        private boolean myIsProcessingOnDemand;
        private final Collection<String> myHiddenNames = new HashSet<String>();
        private final Collection<PsiElement> myCollectedElements = new HashSet<PsiElement>();

        public StaticImportFilteringProcessor(final PsiScopeProcessor delegate) {
            myDelegate = delegate;
        }

        @Override
        public <T> T getHint( final Key<T> hintKey) {
            return myDelegate.getHint(hintKey);
        }

        @Override
        public void handleEvent( final Event event, final Object associated) {
            if (JavaScopeProcessorEvent.SET_CURRENT_FILE_CONTEXT.equals(event) && associated instanceof PsiImportStaticStatement) {
                final PsiImportStaticStatement importStaticStatement = (PsiImportStaticStatement)associated;
                myIsProcessingOnDemand = importStaticStatement.isOnDemand();
                if (!myIsProcessingOnDemand) {
                    myHiddenNames.add(importStaticStatement.getReferenceName());
                }
            }
            myDelegate.handleEvent(event, associated);
        }

        @Override
        public boolean execute( final PsiElement element,  final ResolveState state) {
            if (element instanceof PsiModifierListOwner && ((PsiModifierListOwner)element).hasModifierProperty(PsiModifier.STATIC)) {
                if (element instanceof PsiNamedElement && myIsProcessingOnDemand) {
                    final String name = ((PsiNamedElement)element).getName();
                    if (myHiddenNames.contains(name)) return true;
                }
                if (myCollectedElements.add(element)) {
                    return myDelegate.execute(element, state);
                }
            }
            return true;
        }
    }

    @Override
    public boolean processDeclarations( final PsiScopeProcessor processor,
                                        final ResolveState state,
                                       PsiElement lastParent,
                                        PsiElement place) {
        assert isValid();

        if (processor instanceof ClassResolverProcessor &&
                isPhysical() &&
                (getUserData(PsiFileEx.BATCH_REFERENCE_PROCESSING) == Boolean.TRUE || myResolveCache.hasUpToDateValue())) {
            final ClassResolverProcessor hint = (ClassResolverProcessor)processor;
            String name = hint.getName(state);
            MostlySingularMultiMap<String, SymbolCollectingProcessor.ResultWithContext> cache = myResolveCache.getValue();
            MyResolveCacheProcessor cacheProcessor = new MyResolveCacheProcessor(processor, state);
            return name != null ? cache.processForKey(name, cacheProcessor) : cache.processAllValues(cacheProcessor);
        }

        return processDeclarationsNoGuess(processor, state, lastParent, place);
    }

    private boolean processDeclarationsNoGuess(PsiScopeProcessor processor,  ResolveState state, PsiElement lastParent, PsiElement place) {
        processor.handleEvent(PsiScopeProcessor.Event.SET_DECLARATION_HOLDER, this);
        final ElementClassHint classHint = processor.getHint(ElementClassHint.KEY);
        final NameHint nameHint = processor.getHint(NameHint.KEY);
        final String name = nameHint != null ? nameHint.getName(state) : null;
        final PsiImportList importList = getImportList();

        if (classHint == null || classHint.shouldProcess(ElementClassHint.DeclarationKind.CLASS)) {
            final PsiClass[] classes = getClasses();
            for (PsiClass aClass : classes) {
                if (!processor.execute(aClass, state)) return false;
            }

            final PsiImportStatement[] importStatements = importList.getImportStatements();

            // single-type processing
            for (PsiImportStatement statement : importStatements) {
                if (!statement.isOnDemand()) {
                    if (name != null) {
                        final String refText = statement.getQualifiedName();
                        if (refText == null || !refText.endsWith(name)) continue;
                    }

                    final PsiElement resolved = statement.resolve();
                    if (resolved instanceof PsiClass) {
                        processor.handleEvent(JavaScopeProcessorEvent.SET_CURRENT_FILE_CONTEXT, statement);
                        final PsiClass containingClass = ((PsiClass)resolved).getContainingClass();
                        if (containingClass != null && containingClass.hasTypeParameters()) {
                            if (!processor.execute(resolved, state.put(PsiSubstitutor.KEY,
                                    createRawSubstitutor(containingClass)))) return false;
                        }
                        else if (!processor.execute(resolved, state)) return false;
                    }
                }
            }
            processor.handleEvent(JavaScopeProcessorEvent.SET_CURRENT_FILE_CONTEXT, null);

            // check in current package
            final PsiPackage aPackage = JavaPsiFacade.getInstance(myManager.getProject()).findPackage(getPackageName());
            if (aPackage != null) {
                if (!aPackage.processDeclarations(processor, state, null, place)) {
                    return false;
                }
            }

            // on-demand processing
            for (PsiImportStatement statement : importStatements) {
                if (statement.isOnDemand()) {
                    final PsiElement resolved = statement.resolve();
                    if (resolved != null) {
                        processor.handleEvent(JavaScopeProcessorEvent.SET_CURRENT_FILE_CONTEXT, statement);
                        processOnDemandTarget(resolved, processor, state, place);
                    }
                }
            }
        }

        final PsiImportStaticStatement[] importStaticStatements = importList.getImportStaticStatements();
        if (importStaticStatements.length > 0) {
            final StaticImportFilteringProcessor staticImportProcessor = new StaticImportFilteringProcessor(processor);

            // single member processing
            for (PsiImportStaticStatement importStaticStatement : importStaticStatements) {
                if (importStaticStatement.isOnDemand()) continue;
                final PsiJavaCodeReferenceElement reference = importStaticStatement.getImportReference();
                if (reference != null) {
                    final JavaResolveResult[] results = reference.multiResolve(false);
                    if (results.length > 0) {
                        staticImportProcessor.handleEvent(JavaScopeProcessorEvent.SET_CURRENT_FILE_CONTEXT, importStaticStatement);
                        for (JavaResolveResult result : results) {
                            if (!staticImportProcessor.execute(result.getElement(), state)) return false;
                        }
                    }
                }
            }

            // on-demand processing
            for (PsiImportStaticStatement importStaticStatement : importStaticStatements) {
                if (!importStaticStatement.isOnDemand()) continue;
                final PsiClass targetElement = importStaticStatement.resolveTargetClass();
                if (targetElement != null) {
                    staticImportProcessor.handleEvent(JavaScopeProcessorEvent.SET_CURRENT_FILE_CONTEXT, importStaticStatement);
                    if (!targetElement.processDeclarations(staticImportProcessor, state, lastParent, place)) return false;
                }
            }
        }

        if (classHint == null || classHint.shouldProcess(ElementClassHint.DeclarationKind.CLASS)) {
            processor.handleEvent(JavaScopeProcessorEvent.SET_CURRENT_FILE_CONTEXT, null);

            final PsiJavaCodeReferenceElement[] implicitlyImported = getImplicitlyImportedPackageReferences();
            for (PsiJavaCodeReferenceElement aImplicitlyImported : implicitlyImported) {
                final PsiElement resolved = aImplicitlyImported.resolve();
                if (resolved != null) {
                    if (!processOnDemandTarget(resolved, processor, state, place)) return false;
                }
            }
        }

        return true;
    }

    
    private static PsiSubstitutor createRawSubstitutor(PsiClass containingClass) {
        return JavaPsiFacade.getElementFactory(containingClass.getProject()).createRawSubstitutor(containingClass);
    }

    private static boolean processOnDemandTarget(PsiElement target, PsiScopeProcessor processor, ResolveState substitutor, PsiElement place) {
        if (target instanceof PsiPackage) {
            if (!target.processDeclarations(processor, substitutor, null, place)) {
                return false;
            }
        }
        else if (target instanceof PsiClass) {
            PsiClass[] inners = ((PsiClass)target).getInnerClasses();
            if (((PsiClass)target).hasTypeParameters()) {
                substitutor = substitutor.put(PsiSubstitutor.KEY, createRawSubstitutor((PsiClass)target));
            }

            for (PsiClass inner : inners) {
                if (!processor.execute(inner, substitutor)) return false;
            }
        }
        else {
            LOG.assertTrue(false);
        }
        return true;
    }

    @Override
    public void accept( PsiElementVisitor visitor){
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitJavaFile(this);
        }
        else {
            visitor.visitFile(this);
        }
    }

    @Override
    
    public Language getLanguage() {
        return JavaLanguage.INSTANCE;
    }

    @Override
    public boolean importClass(PsiClass aClass) {
        return JavaCodeStyleManager.getInstance(getProject()).addImport(this, aClass);
    }

    private static final NotNullLazyKey<LanguageLevel, PsiJavaFileBaseImpl> LANGUAGE_LEVEL_KEY = NotNullLazyKey.create("LANGUAGE_LEVEL", new NotNullFunction<PsiJavaFileBaseImpl, LanguageLevel>() {
        @Override
        
        public LanguageLevel fun(PsiJavaFileBaseImpl file) {
            return file.getLanguageLevelInner();
        }
    });

    @Override
    
    public LanguageLevel getLanguageLevel() {
        return LANGUAGE_LEVEL_KEY.getValue(this);
    }

    @Override
    public void clearCaches() {
        super.clearCaches();
        putUserData(LANGUAGE_LEVEL_KEY, null);
    }

    private LanguageLevel getLanguageLevelInner() {
        if (myOriginalFile instanceof PsiJavaFile) {
            return ((PsiJavaFile)myOriginalFile).getLanguageLevel();
        }

        LanguageLevel forcedLanguageLevel = getUserData(PsiUtil.FILE_LANGUAGE_LEVEL_KEY);
        if (forcedLanguageLevel != null) return forcedLanguageLevel;

        VirtualFile virtualFile = getVirtualFile();
        if (virtualFile == null) virtualFile = getUserData(IndexingDataKeys.VIRTUAL_FILE);

        final Project project = getProject();
        if (virtualFile == null) {
            final PsiFile originalFile = getOriginalFile();
            if (originalFile instanceof PsiJavaFile && originalFile != this) {
                return ((PsiJavaFile)originalFile).getLanguageLevel();
            }
            return LanguageLevelProjectExtension.getInstance(project).getLanguageLevel();
        }

        return JavaPsiImplementationHelper.getInstance(project).getEffectiveLanguageLevel(virtualFile);
    }

    private static class MyCacheBuilder implements CachedValueProvider<MostlySingularMultiMap<String, SymbolCollectingProcessor.ResultWithContext>> {
        private final PsiJavaFileBaseImpl myFile;

        public MyCacheBuilder(PsiJavaFileBaseImpl file) {
            myFile = file;
        }

        @Override
        public Result<MostlySingularMultiMap<String, SymbolCollectingProcessor.ResultWithContext>> compute() {
            SymbolCollectingProcessor p = new SymbolCollectingProcessor();
            myFile.processDeclarationsNoGuess(p, ResolveState.initial(), myFile, myFile);
            MostlySingularMultiMap<String, SymbolCollectingProcessor.ResultWithContext> results = p.getResults();
            return Result.create(results, PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT, myFile);
        }
    }

    private static class MyResolveCacheProcessor implements Processor<SymbolCollectingProcessor.ResultWithContext> {
        private final PsiScopeProcessor myProcessor;
        private final ResolveState myState;

        public MyResolveCacheProcessor(PsiScopeProcessor processor, ResolveState state) {
            myProcessor = processor;
            myState = state;
        }

        @Override
        public boolean process(SymbolCollectingProcessor.ResultWithContext result) {
            final PsiElement context = result.getFileContext();
            myProcessor.handleEvent(JavaScopeProcessorEvent.SET_CURRENT_FILE_CONTEXT, context);
            final PsiNamedElement element = result.getElement();

            if (element instanceof PsiClass && context instanceof PsiImportStatement) {
                final PsiClass containingClass = ((PsiClass)element).getContainingClass();
                if (containingClass != null && containingClass.hasTypeParameters()) {
                    return myProcessor.execute(element, myState.put(PsiSubstitutor.KEY, createRawSubstitutor(containingClass)));
                }
            }

            return myProcessor.execute(element, myState);
        }
    }
}
