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

package com.gome.maven.refactoring.rename;

import com.gome.maven.codeInsight.CodeInsightUtilCore;
import com.gome.maven.ide.actions.CopyReferenceAction;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.LanguageNamesValidation;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.undo.BasicUndoableAction;
import com.gome.maven.openapi.command.undo.UndoManager;
import com.gome.maven.openapi.command.undo.UndoableAction;
import com.gome.maven.openapi.command.undo.UnexpectedUndoException;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.PomTargetPsiElement;
import com.gome.maven.psi.*;
import com.gome.maven.psi.meta.PsiMetaData;
import com.gome.maven.psi.meta.PsiMetaOwner;
import com.gome.maven.psi.meta.PsiWritableMetaData;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.refactoring.RefactoringBundle;
import com.gome.maven.refactoring.listeners.RefactoringElementListener;
import com.gome.maven.refactoring.listeners.UndoRefactoringElementListener;
import com.gome.maven.refactoring.util.*;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.usageView.UsageInfoFactory;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.containers.HashMap;
import com.gome.maven.util.containers.MultiMap;

import java.util.*;

public class RenameUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.refactoring.rename.RenameUtil");

    private RenameUtil() {
    }

    
    public static UsageInfo[] findUsages(final PsiElement element,
                                         final String newName,
                                         boolean searchInStringsAndComments,
                                         boolean searchForTextOccurrences,
                                         Map<? extends PsiElement, String> allRenames) {
        final List<UsageInfo> result = Collections.synchronizedList(new ArrayList<UsageInfo>());

        PsiManager manager = element.getManager();
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(manager.getProject());
        RenamePsiElementProcessor processor = RenamePsiElementProcessor.forElement(element);

        Collection<PsiReference> refs = processor.findReferences(element, searchInStringsAndComments);
        for (final PsiReference ref : refs) {
            if (ref == null) {
                LOG.error("null reference from processor " + processor);
                continue;
            }
            PsiElement referenceElement = ref.getElement();
            result.add(new MoveRenameUsageInfo(referenceElement, ref, ref.getRangeInElement().getStartOffset(),
                    ref.getRangeInElement().getEndOffset(), element,
                    ref.resolve() == null && !(ref instanceof PsiPolyVariantReference && ((PsiPolyVariantReference)ref).multiResolve(true).length > 0)));
        }

        processor.findCollisions(element, newName, allRenames, result);

        final PsiElement searchForInComments = processor.getElementToSearchInStringsAndComments(element);

        if (searchInStringsAndComments && searchForInComments != null) {
            String stringToSearch = ElementDescriptionUtil.getElementDescription(searchForInComments, NonCodeSearchDescriptionLocation.STRINGS_AND_COMMENTS);
            if (stringToSearch.length() > 0) {
                final String stringToReplace = getStringToReplace(element, newName, false, processor);
                UsageInfoFactory factory = new NonCodeUsageInfoFactory(searchForInComments, stringToReplace);
                TextOccurrencesUtil.addUsagesInStringsAndComments(searchForInComments, stringToSearch, result, factory);
            }
        }

        if (searchForTextOccurrences && searchForInComments != null) {
            String stringToSearch = ElementDescriptionUtil.getElementDescription(searchForInComments, NonCodeSearchDescriptionLocation.NON_JAVA);
            if (stringToSearch.length() > 0) {
                final String stringToReplace = getStringToReplace(element, newName, true, processor);
                addTextOccurrence(searchForInComments, result, projectScope, stringToSearch, stringToReplace);
            }

            final Pair<String, String> additionalStringToSearch = processor.getTextOccurrenceSearchStrings(searchForInComments, newName);
            if (additionalStringToSearch != null && additionalStringToSearch.first.length() > 0) {
                addTextOccurrence(searchForInComments, result, projectScope, additionalStringToSearch.first, additionalStringToSearch.second);
            }
        }

        return result.toArray(new UsageInfo[result.size()]);
    }

    private static void addTextOccurrence(final PsiElement element, final List<UsageInfo> result, final GlobalSearchScope projectScope,
                                          final String stringToSearch, final String stringToReplace) {
        UsageInfoFactory factory = new UsageInfoFactory() {
            @Override
            public UsageInfo createUsageInfo( PsiElement usage, int startOffset, int endOffset) {
                TextRange textRange = usage.getTextRange();
                int start = textRange == null ? 0 : textRange.getStartOffset();
                return NonCodeUsageInfo.create(usage.getContainingFile(), start + startOffset, start + endOffset, element, stringToReplace);
            }
        };
        TextOccurrencesUtil.addTextOccurences(element, stringToSearch, projectScope, result, factory);
    }


    public static void buildPackagePrefixChangedMessage(final VirtualFile[] virtualFiles, StringBuffer message, final String qualifiedName) {
        if (virtualFiles.length > 0) {
            message.append(RefactoringBundle.message("package.occurs.in.package.prefixes.of.the.following.source.folders.n", qualifiedName));
            for (final VirtualFile virtualFile : virtualFiles) {
                message.append(virtualFile.getPresentableUrl()).append("\n");
            }
            message.append(RefactoringBundle.message("these.package.prefixes.will.be.changed"));
        }
    }

    private static String getStringToReplace(PsiElement element, String newName, boolean nonJava, final RenamePsiElementProcessor theProcessor) {
        if (element instanceof PsiMetaOwner) {
            final PsiMetaOwner psiMetaOwner = (PsiMetaOwner)element;
            final PsiMetaData metaData = psiMetaOwner.getMetaData();
            if (metaData != null) {
                return metaData.getName();
            }
        }

        if (theProcessor != null) {
            String result = theProcessor.getQualifiedNameAfterRename(element, newName, nonJava);
            if (result != null) {
                return result;
            }
        }

        if (element instanceof PsiNamedElement) {
            return newName;
        }
        else {
            LOG.error("Unknown element type");
            return null;
        }
    }

    public static void checkRename(PsiElement element, String newName) throws IncorrectOperationException {
        if (element instanceof PsiCheckedRenameElement) {
            ((PsiCheckedRenameElement)element).checkSetName(newName);
        }
    }

    public static void doRename(final PsiElement element, String newName, UsageInfo[] usages, final Project project,
                                 final RefactoringElementListener listener) throws IncorrectOperationException{
        final RenamePsiElementProcessor processor = RenamePsiElementProcessor.forElement(element);
        final String fqn = element instanceof PsiFile ? ((PsiFile)element).getVirtualFile().getPath() : CopyReferenceAction.elementToFqn(element);
        if (fqn != null) {
            UndoableAction action = new BasicUndoableAction() {
                @Override
                public void undo() throws UnexpectedUndoException {
                    if (listener instanceof UndoRefactoringElementListener) {
                        ((UndoRefactoringElementListener)listener).undoElementMovedOrRenamed(element, fqn);
                    }
                }

                @Override
                public void redo() throws UnexpectedUndoException {
                }
            };
            UndoManager.getInstance(project).undoableActionPerformed(action);
        }
        processor.renameElement(element, newName, usages, listener);
    }

    public static void showErrorMessage(final IncorrectOperationException e, final PsiElement element, final Project project) {
        // may happen if the file or package cannot be renamed. e.g. locked by another application
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            throw new RuntimeException(e);
            //LOG.error(e);
            //return;
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                final String helpID = RenamePsiElementProcessor.forElement(element).getHelpID(element);
                String message = e.getMessage();
                if (StringUtil.isEmpty(message)) {
                    message = RefactoringBundle.message("rename.not.supported");
                }
                CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("rename.title"), message, helpID, project);
            }
        });
    }

    public static void doRenameGenericNamedElement( PsiElement namedElement, String newName, UsageInfo[] usages,
                                                    RefactoringElementListener listener) throws IncorrectOperationException {
        PsiWritableMetaData writableMetaData = null;
        if (namedElement instanceof PsiMetaOwner) {
            final PsiMetaData metaData = ((PsiMetaOwner)namedElement).getMetaData();
            if (metaData instanceof PsiWritableMetaData) {
                writableMetaData = (PsiWritableMetaData)metaData;
            }
        }
        if (writableMetaData == null && !(namedElement instanceof PsiNamedElement)) {
            LOG.error("Unknown element type:" + namedElement);
        }

        boolean hasBindables = false;
        for (UsageInfo usage : usages) {
            if (!(usage.getReference() instanceof BindablePsiReference)) {
                rename(usage, newName);
            } else {
                hasBindables = true;
            }
        }

        if (writableMetaData != null) {
            writableMetaData.setName(newName);
        }
        else {
            PsiElement namedElementAfterRename = ((PsiNamedElement)namedElement).setName(newName);
            if (namedElementAfterRename != null) namedElement = namedElementAfterRename;
        }

        if (hasBindables) {
            for (UsageInfo usage : usages) {
                final PsiReference ref = usage.getReference();
                if (ref instanceof BindablePsiReference) {
                    boolean fallback = true;
                    if (!(ref instanceof FragmentaryPsiReference
                            && ((FragmentaryPsiReference)ref).isFragmentOnlyRename())) {
                        try {
                            ref.bindToElement(namedElement);
                            fallback = false;
                        }
                        catch (IncorrectOperationException ignored) {
                        }
                    }
                    if (fallback) {//fall back to old scheme
                        ref.handleElementRename(newName);
                    }
                }
            }
        }
        if (listener != null) {
            listener.elementRenamed(namedElement);
        }
    }

    public static void rename(UsageInfo info, String newName) throws IncorrectOperationException {
        if (info.getElement() == null) return;
        PsiReference ref = info.getReference();
        if (ref == null) return;
        ref.handleElementRename(newName);
    }

    
    public static List<UnresolvableCollisionUsageInfo> removeConflictUsages(Set<UsageInfo> usages) {
        final List<UnresolvableCollisionUsageInfo> result = new ArrayList<UnresolvableCollisionUsageInfo>();
        for (Iterator<UsageInfo> iterator = usages.iterator(); iterator.hasNext();) {
            UsageInfo usageInfo = iterator.next();
            if (usageInfo instanceof UnresolvableCollisionUsageInfo) {
                result.add((UnresolvableCollisionUsageInfo)usageInfo);
                iterator.remove();
            }
        }
        return result.isEmpty() ? null : result;
    }

    public static void addConflictDescriptions(UsageInfo[] usages, MultiMap<PsiElement, String> conflicts) {
        for (UsageInfo usage : usages) {
            if (usage instanceof UnresolvableCollisionUsageInfo) {
                conflicts.putValue(usage.getElement(), ((UnresolvableCollisionUsageInfo)usage).getDescription());
            }
        }
    }

    public static void renameNonCodeUsages( Project project,  NonCodeUsageInfo[] usages) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        Map<Document, List<UsageOffset>> docsToOffsetsMap = new HashMap<Document, List<UsageOffset>>();
        final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        for (NonCodeUsageInfo usage : usages) {
            PsiElement element = usage.getElement();

            if (element == null) continue;
            element = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(element, true);
            if (element == null) continue;

            final ProperTextRange rangeInElement = usage.getRangeInElement();
            if (rangeInElement == null) continue;

            final PsiFile containingFile = element.getContainingFile();
            final Document document = psiDocumentManager.getDocument(containingFile);

            final Segment segment = usage.getSegment();
            LOG.assertTrue(segment != null);
            int fileOffset = segment.getStartOffset();

            List<UsageOffset> list = docsToOffsetsMap.get(document);
            if (list == null) {
                list = new ArrayList<UsageOffset>();
                docsToOffsetsMap.put(document, list);
            }

            list.add(new UsageOffset(fileOffset, fileOffset + rangeInElement.getLength(), usage.newText));
        }

        for (Document document : docsToOffsetsMap.keySet()) {
            List<UsageOffset> list = docsToOffsetsMap.get(document);
            LOG.assertTrue(list != null, document);
            UsageOffset[] offsets = list.toArray(new UsageOffset[list.size()]);
            Arrays.sort(offsets);

            for (int i = offsets.length - 1; i >= 0; i--) {
                UsageOffset usageOffset = offsets[i];
                document.replaceString(usageOffset.startOffset, usageOffset.endOffset, usageOffset.newText);
            }
            PsiDocumentManager.getInstance(project).commitDocument(document);
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments();
    }

    public static boolean isValidName(final Project project, final PsiElement psiElement, final String newName) {
        if (newName == null || newName.length() == 0) {
            return false;
        }
        final Condition<String> inputValidator = RenameInputValidatorRegistry.getInputValidator(psiElement);
        if (inputValidator != null) {
            return inputValidator.value(newName);
        }
        if (psiElement instanceof PsiFile || psiElement instanceof PsiDirectory) {
            return newName.indexOf('\\') < 0 && newName.indexOf('/') < 0;
        }
        if (psiElement instanceof PomTargetPsiElement) {
            return !StringUtil.isEmptyOrSpaces(newName);
        }

        final PsiFile file = psiElement.getContainingFile();
        final Language elementLanguage = psiElement.getLanguage();

        final Language fileLanguage = file == null ? null : file.getLanguage();
        Language language = fileLanguage == null ? elementLanguage : fileLanguage.isKindOf(elementLanguage) ? fileLanguage : elementLanguage;

        return LanguageNamesValidation.INSTANCE.forLanguage(language).isIdentifier(newName.trim(), project);
    }

    private static class UsageOffset implements Comparable<UsageOffset> {
        final int startOffset;
        final int endOffset;
        final String newText;

        public UsageOffset(int startOffset, int endOffset, String newText) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.newText = newText;
        }

        @Override
        public int compareTo(final UsageOffset o) {
            return startOffset - o.startOffset;
        }
    }
}
