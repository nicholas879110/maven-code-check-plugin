/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.command.undo.BasicUndoableAction;
import com.gome.maven.openapi.command.undo.UndoManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypeManager;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiManagerEx;
import com.gome.maven.psi.impl.file.impl.FileManager;
import com.gome.maven.psi.impl.source.resolve.JavaResolveUtil;
import com.gome.maven.psi.impl.source.tree.FileElement;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.scope.ElementClassHint;
import com.gome.maven.psi.scope.NameHint;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.scope.util.PsiScopesUtil;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.testFramework.LightVirtualFile;

import java.util.LinkedHashMap;
import java.util.StringTokenizer;

public class PsiCodeFragmentImpl extends PsiFileImpl implements JavaCodeFragment, IntentionFilterOwner {
    private final PsiElement myContext;
    private boolean myPhysical;
    private PsiType myThisType;
    private PsiType mySuperType;
    private LinkedHashMap<String, String> myPseudoImports = new LinkedHashMap<String, String>();
    private VisibilityChecker myVisibilityChecker;
    private ExceptionHandler myExceptionHandler;
    private GlobalSearchScope myResolveScope;
    private IntentionActionsFilter myIntentionActionsFilter;

    public PsiCodeFragmentImpl(Project project,
                               IElementType contentElementType,
                               boolean isPhysical,
                                String name,
                               CharSequence text,
                                PsiElement context) {
        super(TokenType.CODE_FRAGMENT,
                contentElementType,
                ((PsiManagerEx)PsiManager.getInstance(project)).getFileManager().createFileViewProvider(
                        new LightVirtualFile(name, FileTypeManager.getInstance().getFileTypeByFileName(name), text), isPhysical)
        );
        myContext = context;
        ((SingleRootFileViewProvider)getViewProvider()).forceCachedPsi(this);
        myPhysical = isPhysical;
    }

    @Override
    
    public Language getLanguage() {
        return getContentElementType().getLanguage();
    }

    @Override
    protected PsiCodeFragmentImpl clone() {
        final PsiCodeFragmentImpl clone = (PsiCodeFragmentImpl)cloneImpl((FileElement)calcTreeElement().clone());
        clone.myPhysical = false;
        clone.myOriginalFile = this;
        clone.myPseudoImports = new LinkedHashMap<String, String>(myPseudoImports);
        FileManager fileManager = ((PsiManagerEx)getManager()).getFileManager();
        SingleRootFileViewProvider cloneViewProvider = (SingleRootFileViewProvider)fileManager.createFileViewProvider(new LightVirtualFile(
                getName(),
                getLanguage(),
                getText()), false);
        cloneViewProvider.forceCachedPsi(clone);
        clone.myViewProvider = cloneViewProvider;
        return clone;
    }

    private FileViewProvider myViewProvider = null;

    @Override
    
    public FileViewProvider getViewProvider() {
        if (myViewProvider != null) return myViewProvider;
        return super.getViewProvider();
    }

    @Override
    public boolean isValid() {
        if (!super.isValid()) return false;
        return myContext == null || myContext.isValid();
    }

    @Override
    
    public FileType getFileType() {
        return StdFileTypes.JAVA;
    }

    @Override
    public PsiElement getContext() {
        return myContext;
    }

    @Override
    public PsiType getThisType() {
        return myThisType;
    }

    @Override
    public void setThisType(PsiType psiType) {
        myThisType = psiType;
    }

    @Override
    public PsiType getSuperType() {
        return mySuperType;
    }

    @Override
    public void setSuperType(final PsiType superType) {
        mySuperType = superType;
    }

    @Override
    public String importsToString() {
        return StringUtil.join(myPseudoImports.values(), ",");
    }

    @Override
    public void addImportsFromString(String imports) {
        StringTokenizer tokenizer = new StringTokenizer(imports, ",");
        while (tokenizer.hasMoreTokens()) {
            String qName = tokenizer.nextToken();
            String name = PsiNameHelper.getShortClassName(qName);
            myPseudoImports.put(name, qName);
        }
    }

    @Override
    public void setVisibilityChecker(VisibilityChecker checker) {
        myVisibilityChecker = checker;
    }

    @Override
    public VisibilityChecker getVisibilityChecker() {
        return myVisibilityChecker;
    }

    @Override
    public boolean isPhysical() {
        return myPhysical;
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor)visitor).visitCodeFragment(this);
        }
        else {
            visitor.visitFile(this);
        }
    }

    @Override
    public boolean processDeclarations( PsiScopeProcessor processor,
                                        ResolveState state,
                                       PsiElement lastParent,
                                        PsiElement place) {
        final ElementClassHint classHint = processor.getHint(ElementClassHint.KEY);

        if (classHint == null || classHint.shouldProcess(ElementClassHint.DeclarationKind.CLASS)) {
            final NameHint nameHint = processor.getHint(NameHint.KEY);
            final String name = nameHint != null ? nameHint.getName(state) : null;
            if (name != null) {
                String qNameImported = myPseudoImports.get(name);
                if (qNameImported != null) {
                    PsiClass imported = JavaPsiFacade.getInstance(myManager.getProject()).findClass(qNameImported, getResolveScope());
                    if (imported != null) {
                        if (!processor.execute(imported, state)) return false;
                    }
                }
            }
            else {
                for (String qNameImported : myPseudoImports.values()) {
                    PsiClass aClass = JavaPsiFacade.getInstance(myManager.getProject()).findClass(qNameImported, getResolveScope());
                    if (aClass != null) {
                        if (!processor.execute(aClass, state)) return false;
                    }
                }
            }

            if (myContext == null) {
                return JavaResolveUtil.processImplicitlyImportedPackages(processor, state, place, getManager());
            }
        }

        IElementType i = myContentElementType;
        if (i == JavaElementType.TYPE_TEXT || i == JavaElementType.EXPRESSION_STATEMENT || i == JavaElementType.REFERENCE_TEXT) {
            return true;
        }
        else {
            processor.handleEvent(PsiScopeProcessor.Event.SET_DECLARATION_HOLDER, this);
            if (lastParent == null) {
                // Parent element should not see our vars
                return true;
            }

            return PsiScopesUtil.walkChildrenScopes(this, processor, state, lastParent, place);
        }
    }

    public String toString() {
        return "PsiCodeFragment:" + getName();
    }

    @Override
    public boolean importClass(PsiClass aClass) {
        final String className = aClass.getName();
        final String qName = aClass.getQualifiedName();
        if (qName == null) return false;
        //if (!myPseudoImports.containsKey(className)){
        myPseudoImports.put(className, qName);
        myManager.beforeChange(false); // to clear resolve caches!
        if (isPhysical()) {
            final Project project = myManager.getProject();
            final Document document = PsiDocumentManager.getInstance(project).getDocument(this);
            UndoManager.getInstance(project).undoableActionPerformed(new ImportClassUndoableAction(className, qName, document, myPseudoImports));
        }
        return true;
        //}
        //else{
        //  return false;
        //}
    }

    private static class ImportClassUndoableAction extends BasicUndoableAction {
        private final String myClassName;
        private final String myQName;
        private final LinkedHashMap<String, String> myPseudoImports;

        public ImportClassUndoableAction(final String className,
                                         final String qName,
                                         final Document document,
                                         final LinkedHashMap<String, String> pseudoImportsMap) {
            super(document);
            myClassName = className;
            myQName = qName;
            myPseudoImports = pseudoImportsMap;
        }

        @Override
        public void undo() {
            myPseudoImports.remove(myClassName);
        }

        @Override
        public void redo() {
            myPseudoImports.put(myClassName, myQName);
        }
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return myExceptionHandler;
    }

    @Override
    public void setIntentionActionsFilter( final IntentionActionsFilter filter) {
        myIntentionActionsFilter = filter;
    }

    @Override
    public IntentionActionsFilter getIntentionActionsFilter() {
        return myIntentionActionsFilter;
    }

    @Override
    public void forceResolveScope(GlobalSearchScope scope) {
        myResolveScope = scope;
    }

    @Override
    public GlobalSearchScope getForcedResolveScope() {
        return myResolveScope;
    }

    @Override
    
    public GlobalSearchScope getResolveScope() {
        if (myResolveScope != null) return myResolveScope;
        return super.getResolveScope();
    }

    @Override
    public void setExceptionHandler(final ExceptionHandler exceptionHandler) {
        myExceptionHandler = exceptionHandler;
    }
}
