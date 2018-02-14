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
package com.gome.maven.psi.util;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.FileASTNode;
import com.gome.maven.lang.Language;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.psi.meta.PsiMetaData;
import com.gome.maven.psi.meta.PsiMetaOwner;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.PsiElementProcessor;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.TimeoutUtil;

import javax.swing.*;
import java.util.Collection;

/**
 * @author yole
 */
public class PsiUtilCore {
    private static final Logger LOG = Logger.getInstance("#com.intellij.psi.util.PsiUtilCore");
    public static final PsiElement NULL_PSI_ELEMENT = new NullPsiElement();
    private static class NullPsiElement implements PsiElement {
        @Override
        
        public Project getProject() {
            throw createException();
        }

        @Override
        
        public Language getLanguage() {
            throw createException();
        }

        @Override
        public PsiManager getManager() {
            throw createException();
        }

        @Override
        
        public PsiElement[] getChildren() {
            throw createException();
        }

        @Override
        public PsiElement getParent() {
            throw createException();
        }

        @Override
        
        public PsiElement getFirstChild() {
            throw createException();
        }

        @Override
        
        public PsiElement getLastChild() {
            throw createException();
        }

        @Override
        
        public PsiElement getNextSibling() {
            throw createException();
        }

        @Override
        
        public PsiElement getPrevSibling() {
            throw createException();
        }

        @Override
        public PsiFile getContainingFile() {
            throw createException();
        }

        @Override
        public TextRange getTextRange() {
            throw createException();
        }

        @Override
        public int getStartOffsetInParent() {
            throw createException();
        }

        @Override
        public int getTextLength() {
            throw createException();
        }

        @Override
        public PsiElement findElementAt(int offset) {
            throw createException();
        }

        @Override
        
        public PsiReference findReferenceAt(int offset) {
            throw createException();
        }

        @Override
        public int getTextOffset() {
            throw createException();
        }

        @Override
        public String getText() {
            throw createException();
        }

        @Override
        
        public char[] textToCharArray() {
            throw createException();
        }

        @Override
        public PsiElement getNavigationElement() {
            throw createException();
        }

        @Override
        public PsiElement getOriginalElement() {
            throw createException();
        }

        @Override
        public boolean textMatches( CharSequence text) {
            throw createException();
        }

        @Override
        public boolean textMatches( PsiElement element) {
            throw createException();
        }

        @Override
        public boolean textContains(char c) {
            throw createException();
        }

        @Override
        public void accept( PsiElementVisitor visitor) {
            throw createException();
        }

        @Override
        public void acceptChildren( PsiElementVisitor visitor) {
            throw createException();
        }

        @Override
        public PsiElement copy() {
            throw createException();
        }

        @Override
        public PsiElement add( PsiElement element) {
            throw createException();
        }

        @Override
        public PsiElement addBefore( PsiElement element, PsiElement anchor) {
            throw createException();
        }

        @Override
        public PsiElement addAfter( PsiElement element, PsiElement anchor) {
            throw createException();
        }

        @Override
        public void checkAdd( PsiElement element) {
            throw createException();
        }

        @Override
        public PsiElement addRange(PsiElement first, PsiElement last) {
            throw createException();
        }

        @Override
        public PsiElement addRangeBefore( PsiElement first,  PsiElement last, PsiElement anchor) {
            throw createException();
        }

        @Override
        public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) {
            throw createException();
        }

        @Override
        public void delete() {
            throw createException();
        }

        @Override
        public void checkDelete() {
            throw createException();
        }

        @Override
        public void deleteChildRange(PsiElement first, PsiElement last) {
            throw createException();
        }

        @Override
        public PsiElement replace( PsiElement newElement) {
            throw createException();
        }

        @Override
        public boolean isValid() {
            throw createException();
        }

        @Override
        public boolean isWritable() {
            throw createException();
        }

        protected PsiInvalidElementAccessException createException() {
            return new PsiInvalidElementAccessException(this, toString(), null);
        }

        @Override
        
        public PsiReference getReference() {
            throw createException();
        }

        @Override
        
        public PsiReference[] getReferences() {
            throw createException();
        }

        @Override
        public <T> T getCopyableUserData(Key<T> key) {
            throw createException();
        }

        @Override
        public <T> void putCopyableUserData(Key<T> key, T value) {
            throw createException();
        }

        @Override
        public boolean processDeclarations( PsiScopeProcessor processor,
                                            ResolveState state,
                                           PsiElement lastParent,
                                            PsiElement place) {
            throw createException();
        }

        @Override
        public PsiElement getContext() {
            throw createException();
        }

        @Override
        public boolean isPhysical() {
            throw createException();
        }

        @Override
        
        public GlobalSearchScope getResolveScope() {
            throw createException();
        }

        @Override
        
        public SearchScope getUseScope() {
            throw createException();
        }

        @Override
        public ASTNode getNode() {
            throw createException();
        }

        @Override
        public <T> T getUserData( Key<T> key) {
            throw createException();
        }

        @Override
        public <T> void putUserData( Key<T> key, T value) {
            throw createException();
        }

        @Override
        public Icon getIcon(int flags) {
            throw createException();
        }

        @Override
        public boolean isEquivalentTo(final PsiElement another) {
            return this == another;
        }

        @Override
        public String toString() {
            return "NULL_PSI_ELEMENT";
        }
    }

    
    public static PsiElement[] toPsiElementArray( Collection<? extends PsiElement> collection) {
        if (collection.isEmpty()) return PsiElement.EMPTY_ARRAY;
        //noinspection SSBasedInspection
        return collection.toArray(new PsiElement[collection.size()]);
    }

    public static Language getNotAnyLanguage(ASTNode node) {
        if (node == null) return Language.ANY;

        final Language lang = node.getElementType().getLanguage();
        return lang == Language.ANY ? getNotAnyLanguage(node.getTreeParent()) : lang;
    }

    
    public static VirtualFile getVirtualFile( PsiElement element) {
        // optimisation: call isValid() on file only to reduce walks up and down
        if (element == null) {
            return null;
        }
        if (element instanceof PsiFileSystemItem) {
            return element.isValid() ? ((PsiFileSystemItem)element).getVirtualFile() : null;
        }
        final PsiFile containingFile = element.getContainingFile();
        if (containingFile == null || !containingFile.isValid()) {
            return null;
        }

        VirtualFile file = containingFile.getVirtualFile();
        if (file == null) {
            PsiFile originalFile = containingFile.getOriginalFile();
            if (originalFile != containingFile && originalFile.isValid()) {
                file = originalFile.getVirtualFile();
            }
        }
        return file;
    }

    public static int compareElementsByPosition(final PsiElement element1, final PsiElement element2) {
        if (element1 != null && element2 != null) {
            final PsiFile psiFile1 = element1.getContainingFile();
            final PsiFile psiFile2 = element2.getContainingFile();
            if (Comparing.equal(psiFile1, psiFile2)){
                final TextRange textRange1 = element1.getTextRange();
                final TextRange textRange2 = element2.getTextRange();
                if (textRange1 != null && textRange2 != null) {
                    return textRange1.getStartOffset() - textRange2.getStartOffset();
                }
            } else if (psiFile1 != null && psiFile2 != null){
                final String name1 = psiFile1.getName();
                final String name2 = psiFile2.getName();
                return name1.compareToIgnoreCase(name2);
            }
        }
        return 0;
    }

    public static boolean hasErrorElementChild( PsiElement element) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof PsiErrorElement) return true;
        }
        return false;
    }

    
    public static PsiElement getElementAtOffset( PsiFile file, int offset) {
        PsiElement elt = file.findElementAt(offset);
        if (elt == null && offset > 0) {
            elt = file.findElementAt(offset - 1);
        }
        if (elt == null) {
            return file;
        }
        return elt;
    }

    
    public static PsiFile getTemplateLanguageFile(final PsiElement element) {
        if (element == null) return null;
        final PsiFile containingFile = element.getContainingFile();
        if (containingFile == null) return null;

        final FileViewProvider viewProvider = containingFile.getViewProvider();
        return viewProvider.getPsi(viewProvider.getBaseLanguage());
    }

    
    public static PsiFile[] toPsiFileArray( Collection<? extends PsiFile> collection) {
        if (collection.isEmpty()) return PsiFile.EMPTY_ARRAY;
        //noinspection SSBasedInspection
        return collection.toArray(new PsiFile[collection.size()]);
    }

    /**
     * @return name for element using element structure info
     */
    
    public static String getName(PsiElement element) {
        String name = null;
        if (element instanceof PsiMetaOwner) {
            final PsiMetaData data = ((PsiMetaOwner) element).getMetaData();
            if (data != null) {
                name = data.getName(element);
            }
        }
        if (name == null && element instanceof PsiNamedElement) {
            name = ((PsiNamedElement) element).getName();
        }
        return name;
    }

    public static String getQualifiedNameAfterRename(String qName, String newName) {
        if (qName == null) return newName;
        int index = qName.lastIndexOf('.');
        return index < 0 ? newName : qName.substring(0, index + 1) + newName;
    }

    public static Language getDialect( PsiElement element) {
        return narrowLanguage(element.getLanguage(), element.getContainingFile().getLanguage());
    }

    protected static Language narrowLanguage(final Language language, final Language candidate) {
        if (candidate.isKindOf(language)) return candidate;
        return language;
    }

    /**
     * Checks if the element is valid. If not, throws {@link com.gome.maven.psi.PsiInvalidElementAccessException} with
     * a meaningful message that points to the reasons why the element is not valid and may contain the stack trace
     * when it was invalidated.
     */
    public static void ensureValid( PsiElement element) {
        if (!element.isValid()) {
            TimeoutUtil.sleep(1); // to see if processing in another thread suddenly makes the element valid again (which is a bug)
            if (element.isValid()) {
                LOG.error("PSI resurrected: " + element + " of " + element.getClass());
                return;
            }
            throw new PsiInvalidElementAccessException(element);
        }
    }

    /**
     * @deprecated use CompletionUtil#getOriginalElement where appropriate instead
     */
    
    public static <T extends PsiElement> T getOriginalElement( T psiElement, final Class<? extends T> elementClass) {
        final PsiFile psiFile = psiElement.getContainingFile();
        final PsiFile originalFile = psiFile.getOriginalFile();
        if (originalFile == psiFile) return psiElement;
        final TextRange range = psiElement.getTextRange();
        final PsiElement element = originalFile.findElementAt(range.getStartOffset());
        final int maxLength = range.getLength();
        T parent = PsiTreeUtil.getParentOfType(element, elementClass, false);
        T next = parent ;
        while (next != null && next.getTextLength() <= maxLength) {
            parent = next;
            next = PsiTreeUtil.getParentOfType(next, elementClass, true);
        }
        return parent;
    }

    
    public static Language findLanguageFromElement(final PsiElement elt) {
        if (!(elt instanceof PsiFile) && elt.getFirstChild() == null) { //is leaf
            final PsiElement parent = elt.getParent();
            if (parent != null) {
                return parent.getLanguage();
            }
        }

        return elt.getLanguage();
    }

    
    public static Language getLanguageAtOffset ( PsiFile file, int offset) {
        final PsiElement elt = file.findElementAt(offset);
        if (elt == null) return file.getLanguage();
        if (elt instanceof PsiWhiteSpace) {
            TextRange textRange = elt.getTextRange();
            if (!textRange.contains(offset)) {
                LOG.error("PSI corrupted: in file "+file+" ("+file.getViewProvider().getVirtualFile()+") offset="+offset+" returned element "+elt+" with text range "+textRange);
            }
            final int decremented = textRange.getStartOffset() - 1;
            if (decremented >= 0) {
                return getLanguageAtOffset(file, decremented);
            }
        }
        return findLanguageFromElement(elt);
    }

    public static Project getProjectInReadAction( final PsiElement element) {
        return ApplicationManager.getApplication().runReadAction(new Computable<Project>() {
            @Override
            public Project compute() {
                return element.getProject();
            }
        });
    }

    
    public static IElementType getElementType( ASTNode node) {
        return node == null ? null : node.getElementType();
    }

    
    public static IElementType getElementType( PsiElement element) {
        return element == null ? null : getElementType(element.getNode());
    }

    public static final PsiFile NULL_PSI_FILE = new NullPsiFile();
    private static class NullPsiFile extends NullPsiElement implements PsiFile {
        @Override
        public FileASTNode getNode() {
            throw createException();
        }

        @Override
        public PsiDirectory getParent() {
            throw createException();
        }

        @Override
        public VirtualFile getVirtualFile() {
            throw createException();
        }

        @Override
        public PsiDirectory getContainingDirectory() {
            throw createException();
        }

        @Override
        public long getModificationStamp() {
            throw createException();
        }

        
        @Override
        public PsiFile getOriginalFile() {
            throw createException();
        }

        
        @Override
        public FileType getFileType() {
            throw createException();
        }

        
        @Override
        public PsiFile[] getPsiRoots() {
            throw createException();
        }

        
        @Override
        public FileViewProvider getViewProvider() {
            throw createException();
        }

        @Override
        public void subtreeChanged() {
            throw createException();
        }

        @Override
        public boolean isDirectory() {
            throw createException();
        }

        
        @Override
        public String getName() {
            throw createException();
        }

        @Override
        public boolean processChildren(PsiElementProcessor<PsiFileSystemItem> processor) {
            throw createException();
        }

        
        @Override
        public ItemPresentation getPresentation() {
            throw createException();
        }

        @Override
        public void navigate(boolean requestFocus) {
            throw createException();
        }

        @Override
        public boolean canNavigate() {
            throw createException();
        }

        @Override
        public boolean canNavigateToSource() {
            throw createException();
        }

        @Override
        public void checkSetName(String name) throws IncorrectOperationException {
            throw createException();
        }

        @Override
        public PsiElement setName(  String name) throws IncorrectOperationException {
            throw createException();
        }

        @Override
        public String toString() {
            return "NULL_PSI_FILE";
        }
    }
}
