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
package com.gome.maven.usages;


import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Factory;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.usages.rules.PsiElementUsage;

/**
 * @author max
 */
public abstract class UsageViewManager {
    public static UsageViewManager getInstance (Project project) {
        return ServiceManager.getService(project, UsageViewManager.class);
    }

    
    public abstract UsageView createUsageView( UsageTarget[] targets,  Usage[] usages,  UsageViewPresentation presentation, Factory<UsageSearcher> usageSearcherFactory);

    
    public abstract UsageView showUsages( UsageTarget[] searchedFor,  Usage[] foundUsages,  UsageViewPresentation presentation, Factory<UsageSearcher> factory);

    
    public abstract UsageView showUsages( UsageTarget[] searchedFor,  Usage[] foundUsages,  UsageViewPresentation presentation);

    public abstract UsageView searchAndShowUsages( UsageTarget[] searchFor,
                                                   Factory<UsageSearcher> searcherFactory,
                                                  boolean showPanelIfOnlyOneUsage,
                                                  boolean showNotFoundMessage,
                                                   UsageViewPresentation presentation,
                                                   UsageViewStateListener listener);

    public interface UsageViewStateListener {
        void usageViewCreated( UsageView usageView);
        void findingUsagesFinished(UsageView usageView);
    }

    public abstract void searchAndShowUsages( UsageTarget[] searchFor,
                                              Factory<UsageSearcher> searcherFactory,
                                              FindUsagesProcessPresentation processPresentation,
                                              UsageViewPresentation presentation,
                                              UsageViewStateListener listener);

    
    public abstract UsageView getSelectedUsageView();

    public static boolean isSelfUsage( final Usage usage,  final UsageTarget[] searchForTarget) {
        if (!(usage instanceof PsiElementUsage)) return false;
        return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                final PsiElement element = ((PsiElementUsage)usage).getElement();
                if (element == null) return false;

                for (UsageTarget ut : searchForTarget) {
                    if (ut instanceof PsiElementUsageTarget) {
                        if (isSelfUsage(element, ((PsiElementUsageTarget)ut).getElement())) {
                            return true;
                        }
                    }
                }
                return false;
            }
        });
    }

    private static boolean isSelfUsage( PsiElement element, PsiElement psiElement) {
        return element.getParent() == psiElement; // self usage might be configurable
    }
}
