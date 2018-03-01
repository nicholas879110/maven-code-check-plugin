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

import com.gome.maven.extapi.psi.StubBasedPsiElementBase;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.navigation.ItemPresentationProviders;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.ui.Queryable;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.*;
import com.gome.maven.psi.impl.java.stubs.JavaStubElementTypes;
import com.gome.maven.psi.impl.java.stubs.PsiClassStub;
import com.gome.maven.psi.impl.source.tree.ChildRole;
import com.gome.maven.psi.impl.source.tree.CompositeElement;
import com.gome.maven.psi.impl.source.tree.SharedImplUtil;
import com.gome.maven.psi.javadoc.PsiDocComment;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.stubs.IStubElementType;
import com.gome.maven.psi.stubs.PsiFileStub;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.util.IncorrectOperationException;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PsiClassImpl extends JavaStubPsiElement<PsiClassStub<?>> implements PsiExtensibleClass, PsiQualifiedNamedElement, Queryable {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.PsiClassImpl");

    private final ClassInnerStuffCache myInnersCache = new ClassInnerStuffCache(this);
    private volatile String myCachedName;

    public PsiClassImpl(final PsiClassStub stub) {
        this(stub, JavaStubElementTypes.CLASS);
    }

    protected PsiClassImpl(final PsiClassStub stub, final IStubElementType type) {
        super(stub, type);
        addTrace(null);
    }

    public PsiClassImpl(final ASTNode node) {
        super(node);
        addTrace(null);
    }

    private void addTrace( PsiClassStub stub) {
        if (ourTraceStubAstBinding) {
            String creationTrace = "Creation thread: " + Thread.currentThread() + "\n" + DebugUtil.currentStackTrace();
            if (stub != null) {
                creationTrace += "\nfrom stub " + stub + "@" + System.identityHashCode(stub) + "\n";
                if (stub instanceof UserDataHolder) {
                    String stubTrace = ((UserDataHolder)stub).getUserData(CREATION_TRACE);
                    if (stubTrace != null) {
                        creationTrace += stubTrace;
                    }
                }
            }
            putUserData(CREATION_TRACE, creationTrace);
        }
    }

    @Override
    public void subtreeChanged() {
        dropCaches();
        super.subtreeChanged();
    }

    private void dropCaches() {
        myInnersCache.dropCaches();
        myCachedName = null;
    }

    @Override
    protected Object clone() {
        PsiClassImpl clone = (PsiClassImpl)super.clone();
        clone.dropCaches();
        return clone;
    }

    @Override
    public PsiElement getParent() {
        final PsiClassStub stub = getStub();
        if (stub != null) {
            final StubElement parentStub = stub.getParentStub();
            if (parentStub instanceof PsiFileStub || parentStub instanceof PsiClassStub
                    ) {
                return parentStub.getPsi();
            }
        }

        return SharedImplUtil.getParent(getNode());
    }

    @Override
    public PsiElement getOriginalElement() {
        final JavaPsiImplementationHelper helper = JavaPsiImplementationHelper.getInstance(getProject());
        if (helper != null) {
            return helper.getOriginalClass(this);
        }
        return this;
    }

    @Override
    
    public CompositeElement getNode() {
        return (CompositeElement)super.getNode();
    }

    @Override
    public PsiIdentifier getNameIdentifier() {
        return (PsiIdentifier)getNode().findChildByRoleAsPsiElement(ChildRole.NAME);
    }

    @Override
    public PsiElement getScope() {
        final PsiClassStub stub = getStub();
        if (stub != null) {
            return stub.getParentStub().getPsi();
        }

        ASTNode treeElement = getNode();
        ASTNode parent = treeElement.getTreeParent();

        while(parent != null) {
            if (parent.getElementType() instanceof IStubElementType){
                return parent.getPsi();
            }
            parent = parent.getTreeParent();
        }

        return getContainingFile();
    }

    @Override
    public String getName() {
        String name = myCachedName;
        if (name != null) return name;
        final PsiClassStub stub = getStub();
        if (stub == null) {
            PsiIdentifier identifier = getNameIdentifier();
            name = identifier == null ? null : identifier.getText();
        }
        else {
            name = stub.getName();
        }
        myCachedName = name;
        return name;
    }

    @Override
    public String getQualifiedName() {
        final PsiClassStub stub = getStub();
        if (stub != null) {
            return stub.getQualifiedName();
        }

        PsiElement parent = getParent();
        if (parent instanceof PsiJavaFile) {
            return StringUtil.getQualifiedName(((PsiJavaFile)parent).getPackageName(), getName());
        }
        if (parent instanceof PsiClass) {
            String parentQName = ((PsiClass)parent).getQualifiedName();
            if (parentQName == null) return null;
            return StringUtil.getQualifiedName(parentQName, getName());
        }

        return null;
    }

    @Override
    public PsiModifierList getModifierList() {
        return getRequiredStubOrPsiChild(JavaStubElementTypes.MODIFIER_LIST);
    }

    @Override
    public boolean hasModifierProperty( String name) {
        final PsiModifierList modlist = getModifierList();
        return modlist != null && modlist.hasModifierProperty(name);
    }

    @Override
    public PsiReferenceList getExtendsList() {
        return getStubOrPsiChild(JavaStubElementTypes.EXTENDS_LIST);
    }

    @Override
    public PsiReferenceList getImplementsList() {
        return getStubOrPsiChild(JavaStubElementTypes.IMPLEMENTS_LIST);
    }

    @Override
    
    public PsiClassType[] getExtendsListTypes() {
        return PsiClassImplUtil.getExtendsListTypes(this);
    }

    @Override
    
    public PsiClassType[] getImplementsListTypes() {
        return PsiClassImplUtil.getImplementsListTypes(this);
    }

    @Override
    public PsiClass getSuperClass() {
        return PsiClassImplUtil.getSuperClass(this);
    }

    @Override
    public PsiClass[] getInterfaces() {
        return PsiClassImplUtil.getInterfaces(this);
    }

    @Override
    
    public PsiClass[] getSupers() {
        return PsiClassImplUtil.getSupers(this);
    }

    @Override
    
    public PsiClassType[] getSuperTypes() {
        return PsiClassImplUtil.getSuperTypes(this);
    }

    @Override
    
    public PsiClass getContainingClass() {
        final PsiClassStub stub = getStub();
        if (stub != null) {
            StubElement parent = stub.getParentStub();
            while (parent != null && !(parent instanceof PsiClassStub)) {
                parent = parent.getParentStub();
            }

            if (parent != null) {
                return ((PsiClassStub<? extends PsiClass>)parent).getPsi();
            }
        }

        PsiElement parent = getParent();

        if (parent instanceof PsiClassLevelDeclarationStatement) {
            return PsiTreeUtil.getParentOfType(this, PsiSyntheticClass.class);
        }

        return parent instanceof PsiClass ? (PsiClass)parent : null;
    }

    @Override
    public PsiElement getContext() {
        final PsiClass cc = getContainingClass();
        return cc != null ? cc : super.getContext();
    }


    @Override
    
    public Collection<HierarchicalMethodSignature> getVisibleSignatures() {
        return PsiSuperMethodImplUtil.getVisibleSignatures(this);
    }

    @Override
    
    public PsiField[] getFields() {
        return myInnersCache.getFields();
    }

    @Override
    
    public PsiMethod[] getMethods() {
        return myInnersCache.getMethods();
    }

    @Override
    
    public PsiMethod[] getConstructors() {
        return myInnersCache.getConstructors();
    }

    @Override
    
    public PsiClass[] getInnerClasses() {
        return myInnersCache.getInnerClasses();
    }

    
    @Override
    public List<PsiField> getOwnFields() {
        return Arrays.asList(getStubOrPsiChildren(Constants.FIELD_BIT_SET, PsiField.ARRAY_FACTORY));
    }

    
    @Override
    public List<PsiMethod> getOwnMethods() {
        return Arrays.asList(getStubOrPsiChildren(Constants.METHOD_BIT_SET, PsiMethod.ARRAY_FACTORY));
    }

    
    @Override
    public List<PsiClass> getOwnInnerClasses() {
        return Arrays.asList(getStubOrPsiChildren(JavaStubElementTypes.CLASS, PsiClass.ARRAY_FACTORY));
    }

    @Override
    
    public PsiClassInitializer[] getInitializers() {
        return getStubOrPsiChildren(JavaStubElementTypes.CLASS_INITIALIZER, PsiClassInitializer.ARRAY_FACTORY);
    }

    @Override
    
    public PsiTypeParameter[] getTypeParameters() {
        return PsiImplUtil.getTypeParameters(this);
    }

    @Override
    
    public PsiField[] getAllFields() {
        return PsiClassImplUtil.getAllFields(this);
    }

    @Override
    
    public PsiMethod[] getAllMethods() {
        return PsiClassImplUtil.getAllMethods(this);
    }

    @Override
    
    public PsiClass[] getAllInnerClasses() {
        return PsiClassImplUtil.getAllInnerClasses(this);
    }

    @Override
    public PsiField findFieldByName(String name, boolean checkBases) {
        return myInnersCache.findFieldByName(name, checkBases);
    }

    @Override
    public PsiMethod findMethodBySignature(PsiMethod patternMethod, boolean checkBases) {
        return PsiClassImplUtil.findMethodBySignature(this, patternMethod, checkBases);
    }

    @Override
    
    public PsiMethod[] findMethodsBySignature(PsiMethod patternMethod, boolean checkBases) {
        return PsiClassImplUtil.findMethodsBySignature(this, patternMethod, checkBases);
    }

    @Override
    
    public PsiMethod[] findMethodsByName(String name, boolean checkBases) {
        return myInnersCache.findMethodsByName(name, checkBases);
    }

    @Override
    
    public List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(String name, boolean checkBases) {
        return PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases);
    }

    @Override
    
    public List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
        return PsiClassImplUtil.getAllWithSubstitutorsByMap(this, PsiClassImplUtil.MemberType.METHOD);
    }

    @Override
    public PsiClass findInnerClassByName(String name, boolean checkBases) {
        return myInnersCache.findInnerClassByName(name, checkBases);
    }

    @Override
    public PsiTypeParameterList getTypeParameterList() {
        return getRequiredStubOrPsiChild(JavaStubElementTypes.TYPE_PARAMETER_LIST);
    }

    @Override
    public boolean hasTypeParameters() {
        return PsiImplUtil.hasTypeParameters(this);
    }

    @Override
    public boolean isDeprecated() {
        final PsiClassStub stub = getStub();
        if (stub != null) {
            return stub.isDeprecated() || stub.hasDeprecatedAnnotation() && PsiImplUtil.isDeprecatedByAnnotation(this);
        }

        return PsiImplUtil.isDeprecatedByDocTag(this) || PsiImplUtil.isDeprecatedByAnnotation(this);
    }

    @Override
    public PsiDocComment getDocComment(){
        return (PsiDocComment)getNode().findChildByRoleAsPsiElement(ChildRole.DOC_COMMENT);
    }

    @Override
    public PsiJavaToken getLBrace() {
        return (PsiJavaToken)getNode().findChildByRoleAsPsiElement(ChildRole.LBRACE);
    }

    @Override
    public PsiJavaToken getRBrace() {
        return (PsiJavaToken)getNode().findChildByRoleAsPsiElement(ChildRole.RBRACE);
    }

    @Override
    public boolean isInterface() {
        final PsiClassStub stub = getStub();
        if (stub != null) {
            return stub.isInterface();
        }

        ASTNode keyword = getNode().findChildByRole(ChildRole.CLASS_OR_INTERFACE_KEYWORD);
        return keyword != null && keyword.getElementType() == JavaTokenType.INTERFACE_KEYWORD;
    }

    @Override
    public boolean isAnnotationType() {
        final PsiClassStub stub = getStub();
        if (stub != null) {
            return stub.isAnnotationType();
        }

        return getNode().findChildByRole(ChildRole.AT) != null;
    }

    @Override
    public boolean isEnum() {
        final PsiClassStub stub = getStub();
        if (stub != null) {
            return stub.isEnum();
        }

        final ASTNode keyword = getNode().findChildByRole(ChildRole.CLASS_OR_INTERFACE_KEYWORD);
        return keyword != null && keyword.getElementType() == JavaTokenType.ENUM_KEYWORD;
    }

    @Override
    public void accept( PsiElementVisitor visitor){
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitClass(this);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public String toString(){
        return "PsiClass:" + getName();
    }

    @Override
    public boolean processDeclarations( PsiScopeProcessor processor,  ResolveState state, PsiElement lastParent,  PsiElement place) {
        if (isEnum()) {
            if (!PsiClassImplUtil.processDeclarationsInEnum(processor, state, myInnersCache)) return false;
        }

        LanguageLevel level = PsiUtil.getLanguageLevel(place);
        return PsiClassImplUtil.processDeclarationsInClass(this, processor, state, null, lastParent, place, level, false);
    }

    @Override
    public PsiElement setName( String newName) throws IncorrectOperationException{
        String oldName = getName();
        boolean isRenameFile = isRenameFileOnRenaming();

        PsiImplUtil.setName(getNameIdentifier(), newName);

        if (isRenameFile) {
            PsiFile file = (PsiFile)getParent();
            String fileName = file.getName();
            int dotIndex = fileName.lastIndexOf('.');
            file.setName(dotIndex >= 0 ? newName + "." + fileName.substring(dotIndex + 1) : newName);
        }

        // rename constructors
        for (PsiMethod method : getConstructors()) {
            if (method.getName().equals(oldName)) {
                method.setName(newName);
            }
        }

        return this;
    }

    private boolean isRenameFileOnRenaming() {
        final PsiElement parent = getParent();
        if (parent instanceof PsiFile) {
            PsiFile file = (PsiFile)parent;
            String fileName = file.getName();
            int dotIndex = fileName.lastIndexOf('.');
            String name = dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName;
            String oldName = getName();
            return name.equals(oldName);
        }
        else {
            return false;
        }
    }

    // optimization to not load tree when resolving bases of anonymous and locals
    // if there is no local classes with such name in scope it's possible to use outer scope as context
    
    public PsiElement calcBasesResolveContext(String baseClassName, final PsiElement defaultResolveContext) {
        return calcBasesResolveContext(this, baseClassName, true, defaultResolveContext);
    }

    private static boolean isAnonymousOrLocal(PsiClass aClass) {
        if (aClass instanceof PsiAnonymousClass) return true;

        final PsiClassStub stub = ((PsiClassImpl)aClass).getStub();
        if (stub != null) {
            final StubElement parentStub = stub.getParentStub();
            return !(parentStub instanceof PsiClassStub || parentStub instanceof PsiFileStub);
        }

        PsiElement parent = aClass.getParent();
        while (parent != null) {
            if (parent instanceof PsiMethod || parent instanceof PsiField || parent instanceof PsiClassInitializer) return true;
            if (parent instanceof PsiClass || parent instanceof PsiFile) return false;
            parent = parent.getParent();
        }

        return false;
    }

    
    private static PsiElement calcBasesResolveContext(PsiClass aClass,
                                                      String className,
                                                      boolean isInitialClass,
                                                      final PsiElement defaultResolveContext) {
        final PsiClassStub stub = ((PsiClassImpl)aClass).getStub();
        if (stub == null || stub.isAnonymousInQualifiedNew()) {
            return aClass.getParent();
        }

        boolean isAnonOrLocal = isAnonymousOrLocal(aClass);

        if (!isAnonOrLocal) {
            return isInitialClass ? defaultResolveContext : aClass;
        }

        if (!isInitialClass) {
            if (aClass.findInnerClassByName(className, true) != null) return aClass;
        }

        final StubElement parentStub = stub.getParentStub();

        final StubBasedPsiElementBase<?> context = (StubBasedPsiElementBase)parentStub.getPsi();
        @SuppressWarnings("unchecked")
        PsiClass[] classesInScope = (PsiClass[])parentStub.getChildrenByType(Constants.CLASS_BIT_SET, PsiClass.ARRAY_FACTORY);

        boolean needPreciseContext = false;
        if (classesInScope.length > 1) {
            for (PsiClass scopeClass : classesInScope) {
                if (scopeClass == aClass) continue;
                String className1 = scopeClass.getName();
                if (className.equals(className1)) {
                    needPreciseContext = true;
                    break;
                }
            }
        }
        else {
            if (classesInScope.length != 1) {
                LOG.assertTrue(classesInScope.length == 1, "Parent stub: "+parentStub.getStubType() +"; children: "+parentStub.getChildrenStubs()+"; \ntext:"+context.getText());
            }
            LOG.assertTrue(classesInScope[0] == aClass);
        }

        if (needPreciseContext) {
            return aClass.getParent();
        }
        else {
            if (context instanceof PsiClass) {
                return calcBasesResolveContext((PsiClass)context, className, false, defaultResolveContext);
            }
            else if (context instanceof PsiMember) {
                return calcBasesResolveContext(((PsiMember)context).getContainingClass(), className, false, defaultResolveContext);
            }
            else {
                LOG.assertTrue(false);
                return context;
            }
        }
    }

    @Override
    public boolean isInheritor( PsiClass baseClass, boolean checkDeep) {
        return InheritanceImplUtil.isInheritor(this, baseClass, checkDeep);
    }

    @Override
    public boolean isInheritorDeep(PsiClass baseClass, PsiClass classToByPass) {
        return InheritanceImplUtil.isInheritorDeep(this, baseClass, classToByPass);
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    public Icon getElementIcon(final int flags) {
        return PsiClassImplUtil.getClassIcon(flags, this);
    }

    @Override
    public boolean isEquivalentTo(final PsiElement another) {
        return PsiClassImplUtil.isClassEquivalentTo(this, another);
    }

    @Override
    
    public SearchScope getUseScope() {
        return PsiClassImplUtil.getClassUseScope(this);
    }

    @Override
    
    public PsiQualifiedNamedElement getContainer() {
        final PsiFile file = getContainingFile();
        final PsiDirectory dir = file.getContainingDirectory();
        return dir == null ? null : JavaDirectoryService.getInstance().getPackage(dir);
    }

    @Override
    public void putInfo( Map<String, String> info) {
        putInfo(this, info);
    }

    public static void putInfo( PsiClass psiClass,  Map<String, String> info) {
        info.put("className", psiClass.getName());
        info.put("qualifiedClassName", psiClass.getQualifiedName());
        PsiFile file = psiClass.getContainingFile();
        if (file instanceof Queryable) {
            ((Queryable)file).putInfo(info);
        }
    }

    @Override
    protected boolean isVisibilitySupported() {
        return true;
    }

    
    public PsiMethod getValuesMethod() {
        return myInnersCache.getValuesMethod();
    }
}
