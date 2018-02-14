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

import com.gome.maven.ide.DataManager;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ReadActionProcessor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.NullableComputable;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiNamedElement;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.search.searches.ReferencesSearch;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;

/**
 * @author peter
 * @see com.gome.maven.find.findUsages.FindUsagesHandlerFactory
 */
public abstract class FindUsagesHandler {
    // return this handler if you want to cancel the search
    
    public static final FindUsagesHandler NULL_HANDLER = new NullFindUsagesHandler();

    
    private final PsiElement myPsiElement;

    protected FindUsagesHandler( PsiElement psiElement) {
        myPsiElement = psiElement;
    }

    
    public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
        return new CommonFindUsagesDialog(myPsiElement, getProject(), getFindUsagesOptions(DataManager.getInstance().getDataContext()),
                toShowInNewTab, mustOpenInNewTab, isSingleFile, this);
    }

    
    public final PsiElement getPsiElement() {
        return myPsiElement;
    }

    
    public final Project getProject() {
        return myPsiElement.getProject();
    }

    
    public PsiElement[] getPrimaryElements() {
        return new PsiElement[]{myPsiElement};
    }

    
    public PsiElement[] getSecondaryElements() {
        return PsiElement.EMPTY_ARRAY;
    }

    
    protected String getHelpId() {
        return FindUsagesManager.getHelpID(myPsiElement);
    }

    
    public static FindUsagesOptions createFindUsagesOptions( Project project,  final DataContext dataContext) {
        FindUsagesOptions findUsagesOptions = new FindUsagesOptions(project, dataContext);
        findUsagesOptions.isUsages = true;
        findUsagesOptions.isSearchForTextOccurrences = true;
        return findUsagesOptions;
    }

    
    public FindUsagesOptions getFindUsagesOptions() {
        return getFindUsagesOptions(null);
    }

    
    public FindUsagesOptions getFindUsagesOptions( final DataContext dataContext) {
        FindUsagesOptions options = createFindUsagesOptions(getProject(), dataContext);
        options.isSearchForTextOccurrences &= isSearchForTextOccurencesAvailable(getPsiElement(), false);
        return options;
    }

    public boolean processElementUsages( final PsiElement element,
                                         final Processor<UsageInfo> processor,
                                         final FindUsagesOptions options) {
        final ReadActionProcessor<PsiReference> refProcessor = new ReadActionProcessor<PsiReference>() {
            @Override
            public boolean processInReadAction(final PsiReference ref) {
                TextRange rangeInElement = ref.getRangeInElement();
                return processor.process(new UsageInfo(ref.getElement(), rangeInElement.getStartOffset(), rangeInElement.getEndOffset(), false));
            }
        };

        final SearchScope scope = options.searchScope;

        final boolean searchText = options.isSearchForTextOccurrences && scope instanceof GlobalSearchScope;

        if (options.isUsages) {
            boolean success =
                    ReferencesSearch.search(new ReferencesSearch.SearchParameters(element, scope, false, options.fastTrack)).forEach(refProcessor);
            if (!success) return false;
        }

        if (searchText) {
            if (options.fastTrack != null) {
                options.fastTrack.searchCustom(new Processor<Processor<PsiReference>>() {
                    @Override
                    public boolean process(Processor<PsiReference> consumer) {
                        return processUsagesInText(element, processor, (GlobalSearchScope)scope);
                    }
                });
            }
            else {
                return processUsagesInText(element, processor, (GlobalSearchScope)scope);
            }
        }
        return true;
    }

    public boolean processUsagesInText( final PsiElement element,
                                        Processor<UsageInfo> processor,
                                        GlobalSearchScope searchScope) {
        Collection<String> stringToSearch = ApplicationManager.getApplication().runReadAction(new NullableComputable<Collection<String>>() {
            @Override
            public Collection<String> compute() {
                return getStringsToSearch(element);
            }
        });
        if (stringToSearch == null) return true;
        return FindUsagesHelper.processUsagesInText(element, stringToSearch, searchScope, processor);
    }

    
    protected Collection<String> getStringsToSearch( final PsiElement element) {
        if (element instanceof PsiNamedElement) {
            return ContainerUtil.createMaybeSingletonList(((PsiNamedElement)element).getName());
        }

        return Collections.singleton(element.getText());
    }

    protected boolean isSearchForTextOccurencesAvailable( PsiElement psiElement, boolean isSingleFile) {
        return false;
    }

    
    public Collection<PsiReference> findReferencesToHighlight( PsiElement target,  SearchScope searchScope) {
        return ReferencesSearch.search(target, searchScope, false).findAll();
    }

    private static class NullFindUsagesHandler extends FindUsagesHandler {
        private NullFindUsagesHandler() {
            super(PsiUtilCore.NULL_PSI_ELEMENT);
        }

        
        @Override
        public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
            throw new IncorrectOperationException();
        }

        
        @Override
        public PsiElement[] getPrimaryElements() {
            throw new IncorrectOperationException();
        }

        
        @Override
        public PsiElement[] getSecondaryElements() {
            throw new IncorrectOperationException();
        }

        
        @Override
        protected String getHelpId() {
            throw new IncorrectOperationException();
        }

        
        @Override
        public FindUsagesOptions getFindUsagesOptions() {
            throw new IncorrectOperationException();
        }

        
        @Override
        public FindUsagesOptions getFindUsagesOptions( DataContext dataContext) {
            throw new IncorrectOperationException();
        }

        @Override
        public boolean processElementUsages( PsiElement element,
                                             Processor<UsageInfo> processor,
                                             FindUsagesOptions options) {
            throw new IncorrectOperationException();
        }

        @Override
        public boolean processUsagesInText( PsiElement element,
                                            Processor<UsageInfo> processor,
                                            GlobalSearchScope searchScope) {
            throw new IncorrectOperationException();
        }

        
        @Override
        protected Collection<String> getStringsToSearch( PsiElement element) {
            throw new IncorrectOperationException();
        }

        @Override
        protected boolean isSearchForTextOccurencesAvailable( PsiElement psiElement, boolean isSingleFile) {
            throw new IncorrectOperationException();
        }

        
        @Override
        public Collection<PsiReference> findReferencesToHighlight( PsiElement target,  SearchScope searchScope) {
            throw new IncorrectOperationException();
        }
    }
}
