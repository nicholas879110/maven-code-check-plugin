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

package com.gome.maven.find.findUsages;

import com.gome.maven.codeInsight.highlighting.HighlightUsagesHandler;
import com.gome.maven.find.FindBundle;
import com.gome.maven.find.FindManager;
import com.gome.maven.find.impl.FindManagerImpl;
import com.gome.maven.lang.findUsages.DescriptiveNameUtil;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.navigation.NavigationItem;
import com.gome.maven.navigation.PsiElementNavigationItem;
import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.actionSystem.DataSink;
import com.gome.maven.openapi.actionSystem.KeyboardShortcut;
import com.gome.maven.openapi.actionSystem.TypeSafeDataProvider;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.Navigatable;
import com.gome.maven.psi.*;
import com.gome.maven.psi.meta.PsiMetaData;
import com.gome.maven.psi.meta.PsiMetaOwner;
import com.gome.maven.psi.meta.PsiPresentableMetaData;
import com.gome.maven.psi.search.LocalSearchScope;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.search.searches.ReferencesSearch;
import com.gome.maven.ui.ComputableIcon;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.usageView.UsageViewBundle;
import com.gome.maven.usageView.UsageViewUtil;
import com.gome.maven.usages.ConfigurableUsageTarget;
import com.gome.maven.usages.PsiElementUsageTarget;
import com.gome.maven.usages.UsageView;
import com.gome.maven.usages.impl.UsageViewImpl;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author max
 */
public class PsiElement2UsageTargetAdapter
        implements PsiElementUsageTarget, TypeSafeDataProvider, PsiElementNavigationItem, ItemPresentation, ConfigurableUsageTarget {
    private final SmartPsiElementPointer myPointer;
     protected final FindUsagesOptions myOptions;

    public PsiElement2UsageTargetAdapter( PsiElement element,  FindUsagesOptions options) {
        myOptions = options;
        myPointer = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);

        if (!(element instanceof NavigationItem)) {
            throw new IllegalArgumentException("Element is not a navigation item: " + element);
        }
        update(element);
    }

    public PsiElement2UsageTargetAdapter( PsiElement element) {
        this(element, new FindUsagesOptions(element.getProject()));
    }

    @Override
    public String getName() {
        PsiElement element = getElement();
        return element instanceof NavigationItem ? ((NavigationItem)element).getName() : null;
    }

    @Override
    
    public ItemPresentation getPresentation() {
        return this;
    }

    @Override
    public void navigate(boolean requestFocus) {
        PsiElement element = getElement();
        if (element instanceof Navigatable && ((Navigatable)element).canNavigate()) {
            ((Navigatable)element).navigate(requestFocus);
        }
    }

    @Override
    public boolean canNavigate() {
        PsiElement element = getElement();
        return element instanceof Navigatable && ((Navigatable)element).canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        PsiElement element = getElement();
        return element instanceof Navigatable && ((Navigatable)element).canNavigateToSource();
    }

    @Override
    public PsiElement getTargetElement() {
        return getElement();
    }

    @Override
    public String toString() {
        return getPresentableText();
    }

    @Override
    public void findUsages() {
        PsiElement element = getElement();
        if (element == null) return;
        ((FindManagerImpl)FindManager.getInstance(element.getProject())).getFindUsagesManager().startFindUsages(element, myOptions, null, null);
    }

    @Override
    public PsiElement getElement() {
        return myPointer.getElement();
    }

    @Override
    public void findUsagesInEditor( FileEditor editor) {
        PsiElement element = getElement();
        FindManager.getInstance(element.getProject()).findUsagesInEditor(element, editor);
    }

    @Override
    public void highlightUsages( PsiFile file,  Editor editor, boolean clearHighlights) {
        PsiElement target = getElement();

        if (file instanceof PsiCompiledFile) file = ((PsiCompiledFile)file).getDecompiledPsiFile();

        Project project = target.getProject();
        final FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(project)).getFindUsagesManager();
        final FindUsagesHandler handler = findUsagesManager.getFindUsagesHandler(target, true);

        // in case of injected file, use host file to highlight all occurrences of the target in each injected file
        PsiFile context = InjectedLanguageManager.getInstance(project).getTopLevelFile(file);
        SearchScope searchScope = new LocalSearchScope(context);
        Collection<PsiReference> refs = handler == null
                ? ReferencesSearch.search(target, searchScope, false).findAll()
                : handler.findReferencesToHighlight(target, searchScope);

        new HighlightUsagesHandler.DoHighlightRunnable(new ArrayList<PsiReference>(refs), project, target,
                editor, context, clearHighlights).run();
    }

    @Override
    public boolean isValid() {
        return getElement() != null;
    }

    @Override
    public boolean isReadOnly() {
        return isValid() && !getElement().isWritable();
    }

    @Override
    public VirtualFile[] getFiles() {
        if (!isValid()) return null;

        final PsiFile psiFile = getElement().getContainingFile();
        if (psiFile == null) return null;

        final VirtualFile virtualFile = psiFile.getVirtualFile();
        return virtualFile == null ? null : new VirtualFile[]{virtualFile};
    }

    
    public static PsiElement2UsageTargetAdapter[] convert( PsiElement[] psiElements) {
        PsiElement2UsageTargetAdapter[] targets = new PsiElement2UsageTargetAdapter[psiElements.length];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = new PsiElement2UsageTargetAdapter(psiElements[i]);
        }

        return targets;
    }

    @Override
    public void calcData(final DataKey key, final DataSink sink) {
        if (key == UsageView.USAGE_INFO_KEY) {
            PsiElement element = getElement();
            if (element != null && element.getTextRange() != null) {
                sink.put(UsageView.USAGE_INFO_KEY, new UsageInfo(element));
            }
        }
        else if (key == UsageView.USAGE_SCOPE) {
            sink.put(UsageView.USAGE_SCOPE, myOptions.searchScope);
        }
    }

    @Override
    public KeyboardShortcut getShortcut() {
        return UsageViewImpl.getShowUsagesWithSettingsShortcut();
    }

    
    @Override
    public String getLongDescriptiveName() {
        SearchScope searchScope = myOptions.searchScope;
        String scopeString = searchScope.getDisplayName();
        PsiElement psiElement = getElement();

        return psiElement == null ? UsageViewBundle.message("node.invalid") :
                FindBundle.message("recent.find.usages.action.popup", StringUtil.capitalize(UsageViewUtil.getType(psiElement)),
                        DescriptiveNameUtil.getDescriptiveName(psiElement),
                        scopeString
                );
    }

    @Override
    public void showSettings() {
        PsiElement element = getElement();
        if (element != null) {
            FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(myPointer.getProject())).getFindUsagesManager();
            findUsagesManager.findUsages(element, null, null, true, null);
        }
    }

    private String myPresentableText;
    private ComputableIcon myIconOpen;
    private ComputableIcon myIconClosed;

    @Override
    public void update() {
        update(getElement());
    }

    private void update(PsiElement element) {
        if (element != null && element.isValid()) {
            final ItemPresentation presentation = ((NavigationItem)element).getPresentation();
            myIconOpen = presentation == null ? null : ComputableIcon.create(presentation, true);
            myIconClosed = presentation == null ? null : ComputableIcon.create(presentation, false);
            myPresentableText = presentation == null ? UsageViewUtil.createNodeText(element) : presentation.getPresentableText();
            if (myIconOpen == null || myIconClosed == null) {
                if (element instanceof PsiMetaOwner) {
                    final PsiMetaOwner psiMetaOwner = (PsiMetaOwner)element;
                    final PsiMetaData metaData = psiMetaOwner.getMetaData();
                    if (metaData instanceof PsiPresentableMetaData) {
                        final PsiPresentableMetaData psiPresentableMetaData = (PsiPresentableMetaData)metaData;
                        if (myIconOpen == null) myIconOpen = ComputableIcon.create(psiPresentableMetaData);
                        if (myIconClosed == null) myIconClosed = ComputableIcon.create(psiPresentableMetaData);
                    }
                }
                else if (element instanceof PsiFile) {
                    final PsiFile psiFile = (PsiFile)element;
                    final VirtualFile virtualFile = psiFile.getVirtualFile();
                    if (virtualFile != null) {
                        myIconOpen = ComputableIcon.create(virtualFile);
                        myIconClosed = ComputableIcon.create(virtualFile);
                    }
                }
            }
        }
    }

    @Override
    public String getPresentableText() {
        return myPresentableText;
    }

    @Override
    public String getLocationString() {
        return null;
    }

    @Override
    public Icon getIcon(boolean open) {
        final ComputableIcon computableIcon = open ? myIconOpen : myIconClosed;
        return computableIcon == null ? null : computableIcon.getIcon();
    }
}
