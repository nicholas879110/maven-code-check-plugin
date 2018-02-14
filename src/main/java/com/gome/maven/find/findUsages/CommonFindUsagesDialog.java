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

import com.gome.maven.lang.HelpID;
import com.gome.maven.lang.findUsages.DescriptiveNameUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.search.LocalSearchScope;
import com.gome.maven.psi.search.PsiSearchHelper;
import com.gome.maven.ui.SimpleColoredComponent;
import com.gome.maven.ui.SimpleTextAttributes;
import com.gome.maven.usageView.UsageViewUtil;
import com.gome.maven.util.ObjectUtils;

import javax.swing.*;

/**
 * @author max
 */
public class CommonFindUsagesDialog extends AbstractFindUsagesDialog {
     protected final PsiElement myPsiElement;
     private final String myHelpId;

    public CommonFindUsagesDialog( PsiElement element,
                                   Project project,
                                   FindUsagesOptions findUsagesOptions,
                                  boolean toShowInNewTab,
                                  boolean mustOpenInNewTab,
                                  boolean isSingleFile,
                                   FindUsagesHandler handler) {
        super(project, findUsagesOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, isTextSearch(element, isSingleFile, handler),
                !isSingleFile && !element.getManager().isInProject(element));
        myPsiElement = element;
        myHelpId = ObjectUtils.chooseNotNull(handler.getHelpId(), HelpID.FIND_OTHER_USAGES);
        init();
    }

    private static boolean isTextSearch( PsiElement element, boolean isSingleFile,  FindUsagesHandler handler) {
        return FindUsagesUtil.isSearchForTextOccurrencesAvailable(element, isSingleFile, handler);
    }

    @Override
    protected boolean isInFileOnly() {
        return super.isInFileOnly() ||
                PsiSearchHelper.SERVICE.getInstance(myPsiElement.getProject()).getUseScope(myPsiElement) instanceof LocalSearchScope;
    }

    @Override
    protected JPanel createFindWhatPanel() {
        return null;
    }

    @Override
    public void configureLabelComponent( SimpleColoredComponent coloredComponent) {
        coloredComponent.append(StringUtil.capitalize(UsageViewUtil.getType(myPsiElement)));
        coloredComponent.append(" ");
        coloredComponent.append(DescriptiveNameUtil.getDescriptiveName(myPsiElement), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }

    
    @Override
    protected String getHelpId() {
        return myHelpId;
    }
}
