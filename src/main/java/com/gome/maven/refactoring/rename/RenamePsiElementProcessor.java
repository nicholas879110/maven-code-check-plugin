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

package com.gome.maven.refactoring.rename;

import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.Pass;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiFileSystemItem;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.search.searches.ReferencesSearch;
import com.gome.maven.refactoring.RefactoringSettings;
import com.gome.maven.refactoring.listeners.RefactoringElementListener;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.containers.MultiMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author yole
 */
public abstract class RenamePsiElementProcessor {
    protected static final ExtensionPointName<RenamePsiElementProcessor> EP_NAME = ExtensionPointName.create("com.gome.maven.renamePsiElementProcessor");

    public abstract boolean canProcessElement( PsiElement element);

    public RenameDialog createRenameDialog(Project project, PsiElement element, PsiElement nameSuggestionContext, Editor editor) {
        return new RenameDialog(project, element, nameSuggestionContext, editor);
    }

    public void renameElement(final PsiElement element, String newName, UsageInfo[] usages,
                               RefactoringElementListener listener) throws IncorrectOperationException {
        RenameUtil.doRenameGenericNamedElement(element, newName, usages, listener);
    }

    
    public Collection<PsiReference> findReferences(final PsiElement element, boolean searchInCommentsAndStrings) {
        return findReferences(element);
    }

    
    public Collection<PsiReference> findReferences(final PsiElement element) {
        return ReferencesSearch.search(element, GlobalSearchScope.projectScope(element.getProject())).findAll();
    }

    
    public Pair<String, String> getTextOccurrenceSearchStrings( PsiElement element,  String newName) {
        return null;
    }

    
    public String getQualifiedNameAfterRename(final PsiElement element, final String newName, final boolean nonJava) {
        return null;
    }

    /**
     * Builds the complete set of elements to be renamed during the refactoring.
     *
     * @param element the base element for the refactoring.
     * @param newName the name into which the element is being renamed.
     * @param allRenames the map (from element to its new name) into which all additional elements to be renamed should be stored.
     */
    public void prepareRenaming(final PsiElement element, final String newName, final Map<PsiElement, String> allRenames) {
        prepareRenaming(element, newName, allRenames, element.getUseScope());
    }

    public void prepareRenaming(final PsiElement element, final String newName, final Map<PsiElement, String> allRenames, SearchScope scope) {
    }

    public void findExistingNameConflicts(final PsiElement element, final String newName, final MultiMap<PsiElement,String> conflicts) {
    }

    public void findExistingNameConflicts(final PsiElement element, final String newName, final MultiMap<PsiElement,String> conflicts, Map<PsiElement, String> allRenames) {
        findExistingNameConflicts(element, newName, conflicts);
    }

    public boolean isInplaceRenameSupported() {
        return true;
    }

    public static List<RenamePsiElementProcessor> allForElement( PsiElement element) {
        final List<RenamePsiElementProcessor> result = new ArrayList<RenamePsiElementProcessor>();
        for (RenamePsiElementProcessor processor : EP_NAME.getExtensions()) {
            if (processor.canProcessElement(element)) {
                result.add(processor);
            }
        }
        return result;
    }

    
    public static RenamePsiElementProcessor forElement( PsiElement element) {
        for(RenamePsiElementProcessor processor: Extensions.getExtensions(EP_NAME)) {
            if (processor.canProcessElement(element)) {
                return processor;
            }
        }
        return DEFAULT;
    }

    
    public Runnable getPostRenameCallback(final PsiElement element, final String newName, final RefactoringElementListener elementListener) {
        return null;
    }

    

    public String getHelpID(final PsiElement element) {
        if (element instanceof PsiFile) {
            return "refactoring.renameFile";
        }
        return "refactoring.renameDialogs";
    }

    public boolean isToSearchInComments(final PsiElement element) {
        return element instanceof PsiFileSystemItem && RefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_FILE;
    }

    public void setToSearchInComments(final PsiElement element, boolean enabled) {
        if (element instanceof PsiFileSystemItem) {
            RefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_FILE = enabled;
        }
    }

    public boolean isToSearchForTextOccurrences(final PsiElement element) {
        return element instanceof PsiFileSystemItem && RefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_FILE;
    }

    public void setToSearchForTextOccurrences(final PsiElement element, boolean enabled) {
        if (element instanceof PsiFileSystemItem) {
            RefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_FILE = enabled;
        }
    }

    public boolean showRenamePreviewButton(final PsiElement psiElement){
        return true;
    }

    /**
     * Returns the element to be renamed instead of the element on which the rename refactoring was invoked (for example, a super method
     * of an inherited method).
     *
     * @param element the element on which the refactoring was invoked.
     * @param editor the editor in which the refactoring was invoked.
     * @return the element to rename, or null if the rename refactoring should be canceled.
     */
    
    public PsiElement substituteElementToRename(final PsiElement element,  Editor editor) {
        return element;
    }

    /**
     * Substitutes element to be renamed and initiate rename procedure. Should be used in order to prevent modal dialogs to appear during inplace rename
     * @param element the element on which refactoring was invoked
     * @param editor the editor in which inplace refactoring was invoked
     * @param renameCallback rename procedure which should be called on the chosen substitution
     */
    public void substituteElementToRename( final PsiElement element,  Editor editor,  Pass<PsiElement> renameCallback) {
        final PsiElement psiElement = substituteElementToRename(element, editor);
        if (psiElement == null) return;
        if (!PsiElementRenameHandler.canRename(psiElement.getProject(), editor, psiElement)) return;
        renameCallback.pass(psiElement);
    }

    public void findCollisions(final PsiElement element, final String newName, final Map<? extends PsiElement, String> allRenames,
                               final List<UsageInfo> result) {
    }

    public static final RenamePsiElementProcessor DEFAULT = new RenamePsiElementProcessor() {
        @Override
        public boolean canProcessElement( final PsiElement element) {
            return true;
        }
    };

    /**
     * Use this method to force showing preview for custom processors.
     * This method is always called after prepareRenaming()
     * @return force show preview
     */
    public boolean forcesShowPreview() {
        return false;
    }

    
    public PsiElement getElementToSearchInStringsAndComments(PsiElement element) {
        return element;
    }
}
